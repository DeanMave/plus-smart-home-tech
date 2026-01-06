package ru.yandex.practicum.commerce.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.contract.payment.PaymentOperations;
import ru.yandex.practicum.commerce.dto.order.OrderDto;
import ru.yandex.practicum.commerce.dto.payment.PaymentDto;
import ru.yandex.practicum.commerce.payment.service.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController implements PaymentOperations {

    private final PaymentService paymentService;

    @Override
    @PostMapping("/productCost")
    public BigDecimal productCost(@RequestBody OrderDto orderDto) {
        log.debug("Запрос на расчет стоимости товаров для заказа: {}", orderDto.getOrderId());
        BigDecimal productCost = paymentService.calculateProductCost(orderDto);
        log.debug("Рассчитанная стоимость товаров: {}", productCost);
        return productCost;
    }

    @Override
    @PostMapping("/totalCost")
    public BigDecimal getTotalCost(@RequestBody OrderDto orderDto) {
        log.debug("Запрос на расчет полной стоимости заказа: {}", orderDto.getOrderId());
        BigDecimal totalCost = paymentService.calculateTotalCost(orderDto);
        log.debug("Полная стоимость заказа: {}", totalCost);
        return totalCost;
    }

    @Override
    @PostMapping
    public PaymentDto payment(@RequestBody OrderDto orderDto) {
        log.debug("Запрос на создание платежа для заказа: {}", orderDto.getOrderId());
        PaymentDto payment = paymentService.createPayment(orderDto);
        log.debug("Создан платеж с ID: {}", payment.getPaymentId());
        return payment;
    }

    @Override
    @PostMapping("/refund")
    public void paymentSuccess(@RequestParam UUID paymentId) {
        log.debug("Запрос на подтверждение успешной оплаты: {}", paymentId);
        paymentService.processPaymentSuccess(paymentId);
        log.debug("Платеж {} успешно переведен в статус SUCCESS", paymentId);
    }

    @Override
    @PostMapping("/failed")
    public void paymentFailed(@RequestParam UUID paymentId) {
        log.debug("Запрос на фиксацию ошибки оплаты: {}", paymentId);
        paymentService.processPaymentFailed(paymentId);
        log.debug("Платеж {} переведен в статус FAILED", paymentId);
    }
}