package ru.yandex.practicum.commerce.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.contract.order.OrderClient;
import ru.yandex.practicum.commerce.contract.shopping.store.ShoppingStoreClient;
import ru.yandex.practicum.commerce.dto.order.OrderDto;
import ru.yandex.practicum.commerce.dto.payment.PaymentDto;
import ru.yandex.practicum.commerce.dto.shopping.store.ProductDto;
import ru.yandex.practicum.commerce.payment.repository.PaymentRepository;
import ru.yandex.practicum.commerce.payment.exception.NoOrderFoundBusinessException;
import ru.yandex.practicum.commerce.payment.exception.NotEnoughInfoInOrderToCalculateBusinessException;
import ru.yandex.practicum.commerce.payment.mapper.PaymentMapper;
import ru.yandex.practicum.commerce.payment.model.PaymentEntity;
import ru.yandex.practicum.commerce.payment.model.PaymentState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final ShoppingStoreClient shoppingStoreClient;
    private final OrderClient orderClient;
    private final PaymentMapper paymentMapper;

    private static final BigDecimal VAT_RATE = new BigDecimal("0.10");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    @Transactional
    public PaymentDto createPayment(OrderDto orderDto) {
        log.info("Создание платежа для заказа: {}", orderDto.getOrderId());
        validateOrderForPayment(orderDto);

        paymentRepository.findByOrderId(orderDto.getOrderId())
                .ifPresent(existingPayment -> {
                    throw new NotEnoughInfoInOrderToCalculateBusinessException(
                            "Платеж для данного заказа уже существует: " + orderDto.getOrderId());
                });

        BigDecimal productCost = calculateProductCost(orderDto);
        BigDecimal deliveryCost = orderDto.getDeliveryPrice();
        BigDecimal taxCost = productCost.multiply(VAT_RATE).setScale(SCALE, ROUNDING_MODE);
        BigDecimal totalCost = calculateTotalCost(orderDto);

        PaymentEntity payment = paymentMapper.createNewPayment(
                orderDto.getOrderId(), productCost, deliveryCost, taxCost, totalCost);

        PaymentEntity savedPayment = paymentRepository.save(payment);

        PaymentDto result = paymentMapper.toDto(savedPayment);
        log.info("Создан платеж с ID: {} для заказа: {}", result.getPaymentId(), orderDto.getOrderId());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateProductCost(OrderDto orderDto) {
        log.info("Расчет стоимости товаров для заказа: {}", orderDto.getOrderId());
        validateOrderForCalculation(orderDto);

        BigDecimal totalProductCost = BigDecimal.ZERO;

        for (Map.Entry<UUID, Integer> entry : orderDto.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();

            ProductDto product = shoppingStoreClient.getProduct(productId);
            BigDecimal productPrice = product.getPrice();

            BigDecimal productTotal = productPrice.multiply(BigDecimal.valueOf(quantity));
            totalProductCost = totalProductCost.add(productTotal);
        }

        log.debug("Рассчитанная стоимость товаров для заказа {}: {}", orderDto.getOrderId(), totalProductCost);
        return totalProductCost.setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalCost(OrderDto orderDto) {
        log.info("Расчет полной стоимости заказа: {}", orderDto.getOrderId());
        validateOrderForCalculation(orderDto);

        BigDecimal productCost = calculateProductCost(orderDto);

        BigDecimal deliveryCost = orderDto.getDeliveryPrice();
        if (deliveryCost == null) {
            throw new NotEnoughInfoInOrderToCalculateBusinessException(
                    "В заказе не указана стоимость доставки: " + orderDto.getOrderId());
        }

        BigDecimal taxCost = productCost.multiply(VAT_RATE).setScale(SCALE, ROUNDING_MODE);

        BigDecimal totalCost = productCost.add(deliveryCost).add(taxCost);

        log.debug("Итоговые расчеты для заказа {}: товары={}, доставка={}, налог={}, итого={}",
                orderDto.getOrderId(), productCost, deliveryCost, taxCost, totalCost);

        return totalCost.setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    @Transactional
    public void processPaymentSuccess(UUID paymentId) {
        log.info("Обработка успешной оплаты: {}", paymentId);

        PaymentEntity payment = getPaymentEntity(paymentId);
        payment.setPaymentState(PaymentState.SUCCESS);

        paymentRepository.save(payment);

        try {
            OrderDto updatedOrder = orderClient.payment(payment.getOrderId());
            if (updatedOrder != null) {
                log.debug("Статус заказа успешно обновлен для заказа: {}", payment.getOrderId());
            } else {
                log.error("Не удалось обновить статус заказа - получен пустой ответ для заказа: {}", payment.getOrderId());
                throw new RuntimeException("Сервис заказов вернул null для заказа: " + payment.getOrderId());
            }
        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса в сервисе заказов для заказа: {}. Ошибка: {}",
                    payment.getOrderId(), e.getMessage());
            throw new RuntimeException("Сбой обновления статуса заказа: " + e.getMessage(), e);
        }
        log.info("Платеж {} успешно отмечен как SUCCESS для заказа: {}", paymentId, payment.getOrderId());
    }

    @Override
    @Transactional
    public void processPaymentFailed(UUID paymentId) {
        log.info("Обработка неудачной оплаты: {}", paymentId);

        PaymentEntity payment = getPaymentEntity(paymentId);
        payment.setPaymentState(PaymentState.FAILED);

        paymentRepository.save(payment);

        try {
            OrderDto updatedOrder = orderClient.paymentFailed(payment.getOrderId());
            if (updatedOrder != null) {
                log.debug("Статус заказа успешно изменен на 'ошибка оплаты' для заказа: {}", payment.getOrderId());
            } else {
                log.error("Не удалось обновить статус заказа - получен пустой ответ для заказа: {}", payment.getOrderId());
                throw new RuntimeException("Сервис заказов вернул null для заказа: " + payment.getOrderId());
            }
        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса заказа в сервисе заказов для заказа: {}. Ошибка: {}",
                    payment.getOrderId(), e.getMessage());
            throw new RuntimeException("Сбой обновления статуса заказа: " + e.getMessage(), e);
        }
        log.info("Платеж {} отмечен как FAILED для заказа: {}", paymentId, payment.getOrderId());
    }

    private PaymentEntity getPaymentEntity(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoOrderFoundBusinessException(paymentId));
    }

    private void validateOrderForCalculation(OrderDto orderDto) {
        if (orderDto == null) {
            throw new NotEnoughInfoInOrderToCalculateBusinessException("Объект заказа не может быть null");
        }

        if (orderDto.getOrderId() == null) {
            throw new NotEnoughInfoInOrderToCalculateBusinessException("ID заказа не может быть null");
        }

        if (orderDto.getProducts() == null || orderDto.getProducts().isEmpty()) {
            throw new NotEnoughInfoInOrderToCalculateBusinessException(
                    "Заказ должен содержать товары для расчета: " + orderDto.getOrderId());
        }
    }

    private void validateOrderForPayment(OrderDto orderDto) {
        validateOrderForCalculation(orderDto);

        if (orderDto.getDeliveryPrice() == null) {
            throw new NotEnoughInfoInOrderToCalculateBusinessException(
                    "Для проведения оплаты необходима стоимость доставки: " + orderDto.getOrderId());
        }
    }
}