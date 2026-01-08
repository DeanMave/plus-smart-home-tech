package ru.practicum.commerce.delivery.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.commerce.delivery.service.DeliveryService;
import ru.yandex.practicum.commerce.contract.delivery.DeliveryOperations;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryDto;
import ru.yandex.practicum.commerce.dto.order.OrderDto;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController implements DeliveryOperations {

    private final DeliveryService deliveryService;

    @Override
    @PutMapping
    public DeliveryDto delivery(@RequestBody DeliveryDto deliveryDto) {
        log.debug("Планирование доставки для заказа: {}", deliveryDto.getOrderId());
        DeliveryDto plannedDelivery = deliveryService.createDelivery(deliveryDto);
        log.debug("Доставка успешно создана с ID: {}", plannedDelivery.getDeliveryId());
        return plannedDelivery;
    }

    @Override
    @PostMapping("/cost")
    public BigDecimal deliveryCost(@RequestBody OrderDto orderDto) {
        log.debug("Расчет стоимости доставки для заказа: {}", orderDto.getOrderId());
        BigDecimal deliveryCost = deliveryService.calculateDeliveryCost(orderDto);
        log.debug("Рассчитанная стоимость доставки: {}", deliveryCost);
        return deliveryCost;
    }

    @Override
    @PostMapping("/picked")
    public void deliveryPicked(@RequestBody UUID orderId) {
        log.debug("Обработка подбора (picked) для заказа: {}", orderId);
        deliveryService.processDeliveryPicked(orderId);
        log.debug("Заказ {} успешно переведен в статус PICKED", orderId);
    }

    @Override
    @PostMapping("/successful")
    public void deliverySuccessful(@RequestBody UUID orderId) {
        log.debug("Обработка успешной доставки для заказа: {}", orderId);
        deliveryService.processDeliverySuccess(orderId);
        log.debug("Доставка для заказа {} завершена успешно", orderId);
    }

    @Override
    @PostMapping("/failed")
    public void deliveryFailed(@RequestBody UUID orderId) {
        log.debug("Обработка ошибки доставки для заказа: {}", orderId);
        deliveryService.processDeliveryFailed(orderId);
        log.debug("Заказ {} переведен в статус ошибки доставки (FAILED)", orderId);
    }
}