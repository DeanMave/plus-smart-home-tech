package ru.yandex.practicum.commerce.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.contract.order.OrderOperations;
import ru.yandex.practicum.commerce.dto.order.CreateNewOrderRequest;
import ru.yandex.practicum.commerce.dto.order.OrderDto;
import ru.yandex.practicum.commerce.dto.order.ProductReturnRequest;
import ru.yandex.practicum.commerce.order.service.OrderService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController implements OrderOperations {

    private final OrderService orderService;

    @Override
    @GetMapping
    public List<OrderDto> getClientOrders(@RequestParam String username) {
        log.debug("Запрос списка заказов для пользователя: {}", username);
        List<OrderDto> orders = orderService.getClientOrders(username);
        log.debug("Возвращено заказов: {}", orders.size());
        return orders;
    }

    @Override
    @PutMapping
    public OrderDto createNewOrder(@RequestParam String username,
                                   @RequestBody CreateNewOrderRequest request) {
        log.debug("Создание нового заказа из корзины: {}", request.getShoppingCart().getShoppingCartId());
        OrderDto order = orderService.createNewOrder(username, request);
        log.debug("Создан заказ с ID: {}", order.getOrderId());
        return order;
    }

    @Override
    @PostMapping("/return")
    public OrderDto productReturn(@RequestBody ProductReturnRequest request) {
        log.debug("Оформление возврата для заказа: {}", request.getOrderId());
        OrderDto order = orderService.productReturn(request);
        log.debug("Заказ обновлен после возврата: {}", order.getOrderId());
        return order;
    }

    @Override
    @PostMapping("/payment")
    public OrderDto payment(@RequestBody UUID orderId) {
        log.debug("Обработка оплаты для заказа: {}", orderId);
        OrderDto order = orderService.payment(orderId);
        log.debug("Статус заказа обновлен после оплаты: {}", order.getOrderId());
        return order;
    }

    @Override
    @PostMapping("/payment/failed")
    public OrderDto paymentFailed(@RequestBody UUID orderId) {
        log.debug("Обработка ошибки оплаты для заказа: {}", orderId);
        OrderDto order = orderService.paymentFailed(orderId);
        log.debug("Статус заказа обновлен (ошибка оплаты): {}", order.getOrderId());
        return order;
    }

    @Override
    @PostMapping("/delivery")
    public OrderDto delivery(@RequestBody UUID orderId) {
        log.debug("Передача заказа в доставку: {}", orderId);
        OrderDto order = orderService.delivery(orderId);
        log.debug("Заказ обновлен после передачи в доставку: {}", order.getOrderId());
        return order;
    }

    @Override
    @PostMapping("/delivery/failed")
    public OrderDto deliveryFailed(@RequestBody UUID orderId) {
        log.debug("Обработка ошибки доставки для заказа: {}", orderId);
        OrderDto order = orderService.deliveryFailed(orderId);
        log.debug("Статус заказа обновлен (ошибка доставки): {}", order.getOrderId());
        return order;
    }

    @Override
    @PostMapping("/completed")
    public OrderDto complete(@RequestBody UUID orderId) {
        log.debug("Завершение заказа: {}", orderId);
        OrderDto order = orderService.complete(orderId);
        log.debug("Заказ успешно завершен: {}", order.getOrderId());
        return order;
    }

    @Override
    @PostMapping("/calculate/total")
    public OrderDto calculateTotalCost(@RequestBody UUID orderId) {
        log.debug("Расчет полной стоимости для заказа: {}", orderId);
        OrderDto order = orderService.calculateTotalCost(orderId);
        log.debug("Расчет завершен для заказа: {}", order.getOrderId());
        return order;
    }

    @Override
    @PostMapping("/calculate/delivery")
    public OrderDto calculateDeliveryCost(@RequestBody UUID orderId) {
        log.debug("Расчет стоимости доставки для заказа: {}", orderId);
        OrderDto order = orderService.calculateDeliveryCost(orderId);
        log.debug("Расчет доставки завершен для заказа: {}", order.getOrderId());
        return order;
    }

    @Override
    @PostMapping("/assembly")
    public OrderDto assembly(@RequestBody UUID orderId) {
        log.debug("Сборка заказа: {}", orderId);
        OrderDto order = orderService.assembly(orderId);
        log.debug("Заказ собран: {}", order.getOrderId());
        return order;
    }

    @Override
    @PostMapping("/assembly/failed")
    public OrderDto assemblyFailed(@RequestBody UUID orderId) {
        log.debug("Ошибка сборки заказа: {}", orderId);
        OrderDto order = orderService.assemblyFailed(orderId);
        log.debug("Статус заказа обновлен (ошибка сборки): {}", order.getOrderId());
        return order;
    }
}
