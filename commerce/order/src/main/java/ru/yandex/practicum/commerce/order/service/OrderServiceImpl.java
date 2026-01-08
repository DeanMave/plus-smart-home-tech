package ru.yandex.practicum.commerce.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.contract.delivery.DeliveryClient;
import ru.yandex.practicum.commerce.contract.payment.PaymentClient;
import ru.yandex.practicum.commerce.contract.warehouse.WarehouseClient;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryDto;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryState;
import ru.yandex.practicum.commerce.dto.order.CreateNewOrderRequest;
import ru.yandex.practicum.commerce.dto.order.OrderDto;
import ru.yandex.practicum.commerce.dto.order.OrderState;
import ru.yandex.practicum.commerce.dto.order.ProductReturnRequest;
import ru.yandex.practicum.commerce.dto.payment.PaymentDto;
import ru.yandex.practicum.commerce.dto.shopping.cart.ShoppingCartDto;
import ru.yandex.practicum.commerce.dto.warehouse.AddressDto;
import ru.yandex.practicum.commerce.dto.warehouse.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.commerce.dto.warehouse.BookedProductsDto;
import ru.yandex.practicum.commerce.order.repository.OrderItemRepository;
import ru.yandex.practicum.commerce.order.repository.OrderRepository;
import ru.yandex.practicum.commerce.order.exception.NoOrderFoundBusinessException;
import ru.yandex.practicum.commerce.order.exception.NotAuthorizedBusinessException;
import ru.yandex.practicum.commerce.order.mapper.OrderMapper;
import ru.yandex.practicum.commerce.order.model.OrderEntity;
import ru.yandex.practicum.commerce.order.model.OrderItemEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WarehouseClient warehouseClient;
    private final PaymentClient paymentClient;
    private final DeliveryClient deliveryClient;
    private final OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getClientOrders(String username) {
        validateUsername(username);
        log.info("Получение списка заказов для пользователя: {}", username);
        List<OrderEntity> orders = orderRepository.findByUsernameOrderByCreatedAtDesc(username);

        return orders.stream()
                .map(order -> {
                    List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
                    return orderMapper.toDto(order, items);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDto createNewOrder(String username, CreateNewOrderRequest request) {
        log.info("Создание нового заказа из корзины: {}", request.getShoppingCart().getShoppingCartId());
        validateUsername(username);

        ShoppingCartDto tempCart = ShoppingCartDto.builder()
                .shoppingCartId(request.getShoppingCart().getShoppingCartId())
                .products(new HashMap<>(request.getShoppingCart().getProducts()))
                .build();

        BookedProductsDto bookedProductsDto;
        try {
            bookedProductsDto = warehouseClient.checkProductQuantityEnoughForShoppingCart(tempCart);
            log.debug("Наличие товаров подтверждено складом");
        } catch (Exception e) {
            log.error("Ошибка при проверке наличия товаров на складе: {}", e.getMessage());
            throw new RuntimeException("Проверка наличия товаров не удалась: " + e.getMessage(), e);
        }

        OrderEntity order = OrderEntity.builder()
                .username(username)
                .shoppingCartId(request.getShoppingCart().getShoppingCartId())
                .orderState(OrderState.NEW)
                .deliveryWeight(bookedProductsDto.getDeliveryWeight())
                .deliveryVolume(bookedProductsDto.getDeliveryVolume())
                .fragile(bookedProductsDto.getFragile())
                .country(request.getDeliveryAddress().getCountry())
                .city(request.getDeliveryAddress().getCity())
                .street(request.getDeliveryAddress().getStreet())
                .house(request.getDeliveryAddress().getHouse())
                .flat(request.getDeliveryAddress().getFlat())
                .build();

        OrderEntity savedOrder = orderRepository.save(order);

        List<OrderItemEntity> orderItems = orderMapper.toOrderItemEntities(
                savedOrder,
                request.getShoppingCart().getProducts()
        );
        orderItemRepository.saveAll(orderItems);

        AssemblyProductsForOrderRequest assemblyRequest = AssemblyProductsForOrderRequest.builder()
                .orderId(savedOrder.getOrderId())
                .products(request.getShoppingCart().getProducts())
                .build();

        try {
            BookedProductsDto assemblyResult = warehouseClient.assemblyProductsForOrder(assemblyRequest);
            log.debug("Товары собраны для заказа: {}", savedOrder.getOrderId());

            assembly(savedOrder.getOrderId());

            savedOrder.setDeliveryWeight(assemblyResult.getDeliveryWeight());
            savedOrder.setDeliveryVolume(assemblyResult.getDeliveryVolume());
            savedOrder.setFragile(assemblyResult.getFragile());

        } catch (Exception e) {
            log.error("Ошибка сборки товаров для заказа {}: {}", savedOrder.getOrderId(), e.getMessage());
            assemblyFailed(savedOrder.getOrderId());
            throw new RuntimeException("Сборка товаров не удалась: " + e.getMessage(), e);
        }

        DeliveryDto createdDelivery;
        try {
            AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();
            log.debug("Получен адрес склада: {}", warehouseAddress);

            DeliveryDto deliveryRequest = DeliveryDto.builder()
                    .orderId(savedOrder.getOrderId())
                    .fromAddress(warehouseAddress)
                    .toAddress(request.getDeliveryAddress())
                    .deliveryState(DeliveryState.CREATED)
                    .build();

            createdDelivery = deliveryClient.delivery(deliveryRequest);
            savedOrder.setDeliveryId(createdDelivery.getDeliveryId());
            log.debug("Доставка создана с ID: {}", createdDelivery.getDeliveryId());
        } catch (Exception e) {
            log.error("Ошибка при создании доставки: {}", e.getMessage());
            throw new RuntimeException("Создание доставки не удалось: " + e.getMessage(), e);
        }

        OrderDto orderDtoForDelivery = orderMapper.toDto(savedOrder, orderItems);
        BigDecimal deliveryCost;
        try {
            deliveryCost = deliveryClient.deliveryCost(orderDtoForDelivery);
            savedOrder.setDeliveryPrice(deliveryCost);
            log.debug("Рассчитана стоимость доставки: {}", deliveryCost);
        } catch (Exception e) {
            log.error("Ошибка при расчете стоимости доставки: {}", e.getMessage());
            throw new RuntimeException("Расчет доставки не удался: " + e.getMessage(), e);
        }

        OrderEntity updatedOrder = orderRepository.save(savedOrder);

        OrderDto orderDtoForPayment = orderMapper.toDto(updatedOrder, orderItems);
        PaymentDto paymentDto;
        try {
            paymentDto = paymentClient.payment(orderDtoForPayment);
            updatedOrder.setPaymentId(paymentDto.getPaymentId());
            updatedOrder.setOrderState(OrderState.ON_PAYMENT);
            updatedOrder.setTotalPrice(paymentDto.getTotalPayment());
            updatedOrder.setProductPrice(paymentDto.getTotalPayment().subtract(paymentDto.getDeliveryTotal())
                    .subtract(paymentDto.getFeeTotal()));
            log.debug("Процесс оплаты запущен, ID платежа: {}", paymentDto.getPaymentId());
        } catch (Exception e) {
            log.error("Ошибка при создании платежа: {}", e.getMessage());
            throw new RuntimeException("Создание платежа не удалось: " + e.getMessage(), e);
        }

        OrderEntity finalOrder = orderRepository.save(updatedOrder);
        OrderDto result = orderMapper.toDto(finalOrder, orderItems);

        log.info("Создан новый заказ с ID: {} и ID платежа: {}",
                result.getOrderId(), result.getPaymentId());

        return result;
    }

    @Override
    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        log.info("Обработка возврата товаров для заказа: {}", request.getOrderId());

        OrderEntity order = getOrderEntity(request.getOrderId());
        order.setOrderState(OrderState.PRODUCT_RETURNED);

        OrderEntity updatedOrder = orderRepository.save(order);
        List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(updatedOrder.getOrderId());

        try {
            Map<UUID, Integer> returnedProducts = new HashMap<>(request.getProducts());
            warehouseClient.acceptReturn(returnedProducts);
            log.debug("Товары успешно возвращены на склад: {}", returnedProducts);
        } catch (Exception e) {
            log.error("Ошибка при возврате товаров на склад: {}", e.getMessage());
            throw new RuntimeException("Возврат товаров на склад не удался: " + e.getMessage(), e);
        }
        return orderMapper.toDto(updatedOrder, items);
    }

    @Override
    @Transactional
    public OrderDto payment(UUID orderId) {
        log.info("Обработка успешной оплаты заказа: {}", orderId);

        OrderEntity order = getOrderEntity(orderId);
        if (order.getOrderState() != OrderState.ON_PAYMENT) {
            log.warn("Заказ {} находится в статусе {}, ожидался ON_PAYMENT",
                    orderId, order.getOrderState());
        }
        order.setOrderState(OrderState.PAID);

        OrderEntity updatedOrder = orderRepository.save(order);
        List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(updatedOrder.getOrderId());
        log.info("Заказ {} успешно оплачен", orderId);

        try {
            deliveryClient.deliveryPicked(orderId);
            log.debug("Служба доставки уведомлена о готовности заказа: {}", orderId);
        } catch (Exception e) {
            log.error("Ошибка при вызове службы доставки после оплаты заказа: {}. Ошибка: {}",
                    orderId, e.getMessage());
            throw new RuntimeException("Сбой при уведомлении службы доставки: " + e.getMessage(), e);
        }
        log.info("Запрос на доставку заказа {} успешно отправлен", orderId);
        return orderMapper.toDto(updatedOrder, items);
    }

    @Override
    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        log.info("Обработка ошибки оплаты заказа: {}", orderId);

        OrderEntity order = getOrderEntity(orderId);
        order.setOrderState(OrderState.PAYMENT_FAILED);

        OrderEntity updatedOrder = orderRepository.save(order);
        List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(updatedOrder.getOrderId());
        log.info("Зафиксирована ошибка оплаты заказа {}", orderId);
        return orderMapper.toDto(updatedOrder, items);
    }

    @Override
    @Transactional
    public OrderDto delivery(UUID orderId) {
        log.info("Обработка доставки заказа: {}", orderId);

        OrderEntity order = getOrderEntity(orderId);
        order.setOrderState(OrderState.DELIVERED);

        OrderEntity updatedOrder = orderRepository.save(order);
        List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(updatedOrder.getOrderId());

        return orderMapper.toDto(updatedOrder, items);
    }

    @Override
    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        log.info("Обработка ошибки доставки заказа: {}", orderId);

        OrderEntity order = getOrderEntity(orderId);
        order.setOrderState(OrderState.DELIVERY_FAILED);

        OrderEntity updatedOrder = orderRepository.save(order);
        List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(updatedOrder.getOrderId());

        return orderMapper.toDto(updatedOrder, items);
    }

    @Override
    @Transactional
    public OrderDto complete(UUID orderId) {
        log.info("Завершение заказа: {}", orderId);

        OrderEntity order = getOrderEntity(orderId);
        order.setOrderState(OrderState.COMPLETED);

        OrderEntity updatedOrder = orderRepository.save(order);
        List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(updatedOrder.getOrderId());

        return orderMapper.toDto(updatedOrder, items);
    }

    @Override
    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        log.info("Расчет полной стоимости заказа: {}", orderId);

        OrderEntity order = getOrderEntity(orderId);
        List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(orderId);
        OrderDto orderDto = orderMapper.toDto(order, items);

        BigDecimal deliveryPrice = order.getDeliveryPrice();

        if (deliveryPrice == null) {
            try {
                deliveryPrice = deliveryClient.deliveryCost(orderDto);
                order.setDeliveryPrice(deliveryPrice);
                orderDto.setDeliveryPrice(deliveryPrice);
            } catch (Exception e) {
                log.warn("Не удалось получить цену доставки при расчете общей стоимости");
            }
        }
        BigDecimal totalCost;
        try {
            totalCost = paymentClient.getTotalCost(orderDto);
        } catch (Exception e) {
            log.error("Ошибка расчета общей стоимости...");
            throw new RuntimeException("Расчет общей стоимости не удался: " + e.getMessage(), e);
        }
        order.setTotalPrice(totalCost);
        OrderEntity updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder, items);
    }

    @Override
    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        log.info("Расчет стоимости доставки заказа: {}", orderId);

        OrderEntity order = getOrderEntity(orderId);
        List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(orderId);
        OrderDto orderDto = orderMapper.toDto(order, items);

        if (order.getTotalPrice() == null) {
            BigDecimal totalCost = calculateTotalCost(orderId).getTotalPrice();
            order.setTotalPrice(totalCost);
            orderDto.setTotalPrice(totalCost);
            log.debug("Общая стоимость рассчитана в процессе определения цены доставки: {}", totalCost);
        }

        BigDecimal deliveryCost;
        try {
            deliveryCost = deliveryClient.deliveryCost(orderDto);
            log.debug("Стоимость доставки успешно получена от сервиса доставки: {}", deliveryCost);
        } catch (Exception e) {
            log.error("Ошибка расчета доставки в сервисе доставки для заказа: {}. Ошибка: {}",
                    orderId, e.getMessage());
            throw new RuntimeException("Расчет доставки не удался: " + e.getMessage(), e);
        }

        order.setDeliveryPrice(deliveryCost);
        OrderEntity updatedOrder = orderRepository.save(order);

        return orderMapper.toDto(updatedOrder, items);
    }

    @Override
    @Transactional
    public OrderDto assembly(UUID orderId) {
        log.info("Обработка сборки заказа: {}", orderId);

        OrderEntity order = getOrderEntity(orderId);
        order.setOrderState(OrderState.ASSEMBLED);

        OrderEntity updatedOrder = orderRepository.save(order);
        List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(updatedOrder.getOrderId());

        return orderMapper.toDto(updatedOrder, items);
    }

    @Override
    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        log.info("Обработка ошибки сборки заказа: {}", orderId);

        OrderEntity order = getOrderEntity(orderId);
        order.setOrderState(OrderState.ASSEMBLY_FAILED);

        OrderEntity updatedOrder = orderRepository.save(order);
        List<OrderItemEntity> items = orderItemRepository.findByOrderOrderId(updatedOrder.getOrderId());

        return orderMapper.toDto(updatedOrder, items);
    }

    private OrderEntity getOrderEntity(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundBusinessException(orderId));
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedBusinessException("Имя пользователя не может быть пустым");
        }
    }
}
