package ru.practicum.commerce.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.commerce.delivery.repository.DeliveryRepository;
import ru.practicum.commerce.delivery.exception.NoDeliveryFoundBusinessException;
import ru.practicum.commerce.delivery.mapper.DeliveryMapper;
import ru.practicum.commerce.delivery.model.DeliveryEntity;
import ru.yandex.practicum.commerce.contract.order.OrderClient;
import ru.yandex.practicum.commerce.contract.warehouse.WarehouseClient;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryDto;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryState;
import ru.yandex.practicum.commerce.dto.order.OrderDto;
import ru.yandex.practicum.commerce.dto.warehouse.AddressDto;
import ru.yandex.practicum.commerce.dto.warehouse.ShippedToDeliveryRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final WarehouseClient warehouseClient;
    private final OrderClient orderClient;
    private final DeliveryMapper deliveryMapper;

    private static final BigDecimal BASE_COST = new BigDecimal("5.0");
    private static final BigDecimal FRAGILE_MULTIPLIER = new BigDecimal("0.2");
    private static final BigDecimal WEIGHT_MULTIPLIER = new BigDecimal("0.3");
    private static final BigDecimal VOLUME_MULTIPLIER = new BigDecimal("0.2");
    private static final BigDecimal ADDRESS_MULTIPLIER = new BigDecimal("0.2");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    @Transactional
    public DeliveryDto createDelivery(DeliveryDto deliveryDto) {
        log.info("Создание записи о доставке для заказа: {}", deliveryDto.getOrderId());

        deliveryRepository.findByOrderId(deliveryDto.getOrderId())
                .ifPresent(existingDelivery -> {
                    throw new RuntimeException("Доставка для заказа " + deliveryDto.getOrderId() + " уже существует");
                });

        DeliveryEntity deliveryEntity = deliveryMapper.toEntity(deliveryDto);
        DeliveryEntity savedDelivery = deliveryRepository.save(deliveryEntity);

        DeliveryDto result = deliveryMapper.toDto(savedDelivery);
        log.info("Создана доставка с ID: {} для заказа: {}", result.getDeliveryId(), deliveryDto.getOrderId());

        return result;
    }

    @Override
    @Transactional
    public BigDecimal calculateDeliveryCost(OrderDto orderDto) {
        UUID orderId = orderDto.getOrderId();
        log.info("[Заказ: {}] Начало расчёта стоимости доставки", orderId);

        AddressDto warehouseAddressDto;
        try {
            warehouseAddressDto = warehouseClient.getWarehouseAddress();
            log.debug("Получен адрес склада: {}", warehouseAddressDto.getStreet());
        } catch (Exception e) {
            log.error("Не удалось получить адрес склада: {}", e.getMessage());
            throw new RuntimeException("Ошибка получения адреса склада: " + e.getMessage(), e);
        }

        String deliveryStreet = getDeliveryStreetFromDatabase(orderDto.getOrderId());

        BigDecimal cost = calculateDeliveryCostAlgorithm(
                orderId,
                warehouseAddressDto.getStreet(),
                orderDto.getDeliveryWeight(),
                orderDto.getDeliveryVolume(),
                orderDto.getFragile(),
                deliveryStreet
        );

        updateDeliveryCostByOrderId(orderDto.getOrderId(), cost);

        log.debug("Рассчитана стоимость доставки для заказа {}: {}", orderDto.getOrderId(), cost);
        return cost.setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    @Transactional
    public void processDeliveryPicked(UUID orderId) {
        log.info("Обработка подбора заказа курьером: {}", orderId);

        DeliveryEntity delivery = getDeliveryByOrderIdEntity(orderId);

        ShippedToDeliveryRequest shippedRequest = ShippedToDeliveryRequest.builder()
                .orderId(orderId)
                .deliveryId(delivery.getDeliveryId())
                .build();
        try {
            warehouseClient.shippedToDelivery(shippedRequest);
            delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
            deliveryRepository.save(delivery);
            log.debug("Склад уведомлен об отгрузке заказа: {}", orderId);

        } catch (Exception e) {
            log.error("Ошибка при обработке подбора заказа: {}. Ошибка: {}", orderId, e.getMessage());
            delivery.setDeliveryState(DeliveryState.FAILED);
            deliveryRepository.save(delivery);
            throw new RuntimeException("Не удалось обработать подбор заказа: " + e.getMessage(), e);
        }
        log.info("Заказ {} переведен в статус 'В ПУТИ'", orderId);
    }

    @Override
    @Transactional
    public void processDeliverySuccess(UUID orderId) {
        log.info("Обработка успешного завершения доставки заказа: {}", orderId);

        DeliveryEntity delivery = getDeliveryByOrderIdEntity(orderId);
        delivery.setDeliveryState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);

        try {
            OrderDto updatedOrder = orderClient.delivery(orderId);
            if (updatedOrder != null) {
                log.debug("Статус заказа успешно обновлен на 'Доставлен': {}", orderId);
            } else {
                log.error("Сервис заказов вернул пустой ответ для заказа: {}", orderId);
                throw new RuntimeException("Сервис заказов вернул null для заказа: " + orderId);
            }
        } catch (Exception e) {
            log.error("Ошибка обновления статуса заказа в OrderService: {}. Ошибка: {}", orderId, e.getMessage());
            throw new RuntimeException("Ошибка обновления статуса заказа: " + e.getMessage(), e);
        }
        log.info("Доставка заказа {} успешно завершена", orderId);
    }

    @Override
    @Transactional
    public void processDeliveryFailed(UUID orderId) {
        log.info("Обработка ошибки доставки заказа: {}", orderId);

        DeliveryEntity delivery = getDeliveryByOrderIdEntity(orderId);
        delivery.setDeliveryState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);

        try {
            OrderDto updatedOrder = orderClient.deliveryFailed(orderId);
            if (updatedOrder != null) {
                log.debug("Статус заказа успешно изменен на 'Ошибка доставки': {}", orderId);
            } else {
                log.error("Сервис заказов вернул пустой ответ при ошибке доставки заказа: {}", orderId);
                throw new RuntimeException("Сервис заказов вернул null для заказа: " + orderId);
            }
        } catch (Exception e) {
            log.error("Ошибка обновления статуса заказа в OrderService: {}. Ошибка: {}", orderId, e.getMessage());
            throw new RuntimeException("Ошибка обновления статуса заказа: " + e.getMessage(), e);
        }
        log.info("Заказ {} помечен как недоставленный (FAILED)", orderId);
    }

    private DeliveryEntity getDeliveryByOrderIdEntity(UUID orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundBusinessException(null,
                        "Доставка для заказа " + orderId + " не найдена"));
    }

    private String getDeliveryStreetFromDatabase(UUID orderId) {
        try {
            DeliveryEntity delivery = getDeliveryByOrderIdEntity(orderId);

            if (delivery.getToAddress() != null && delivery.getToAddress().getStreet() != null) {
                return delivery.getToAddress().getStreet();
            } else {
                log.warn("Адрес доставки не найден для заказа: {}, используется пустая строка", orderId);
                return "";
            }
        } catch (NoDeliveryFoundBusinessException e) {
            log.warn("Запись о доставке отсутствует для заказа: {}, невозможно получить адрес", orderId);
            throw new RuntimeException("Запись о доставке не найдена для заказа: " + orderId + ". Сначала создайте доставку.");
        }
    }

    private BigDecimal calculateDeliveryCostAlgorithm(UUID orderId,
                                                      String warehouseAddress,
                                                      Double weight,
                                                      Double volume,
                                                      Boolean fragile,
                                                      String deliveryStreet) {
        log.debug("[Заказ: {}] Входные данные алгоритма: вес={}, объём={}, хрупкость={}, адрес={}",
                orderId, weight, volume, fragile, deliveryStreet);

        BigDecimal cost = BASE_COST;

        BigDecimal addressMultiplier = BigDecimal.ONE;
        if (warehouseAddress != null) {
            if (warehouseAddress.contains("ADDRESS_1")) {
                addressMultiplier = BigDecimal.ONE;
            } else if (warehouseAddress.contains("ADDRESS_2")) {
                addressMultiplier = new BigDecimal("2");
            }
        }
        cost = cost.multiply(addressMultiplier).add(BASE_COST);
        log.debug("[Заказ: {}] База + складской множитель ({}). Промежуточный итог: {}", orderId, addressMultiplier, cost);

        if (Boolean.TRUE.equals(fragile)) {
            BigDecimal fragileCost = cost.multiply(FRAGILE_MULTIPLIER);
            cost = cost.add(fragileCost);
            log.debug("[Заказ: {}] Наценка за хрупкость (+{}). Промежуточный итог: {}", orderId, fragileCost, cost);
        }

        if (weight != null && weight > 0) {
            BigDecimal weightCost = BigDecimal.valueOf(weight).multiply(WEIGHT_MULTIPLIER);
            cost = cost.add(weightCost);
            log.debug("[Заказ: {}] Наценка за вес {} кг (+{}). Промежуточный итог: {}", orderId, weight, weightCost, cost);
        }

        if (volume != null && volume > 0) {
            BigDecimal volumeCost = BigDecimal.valueOf(volume).multiply(VOLUME_MULTIPLIER);
            cost = cost.add(volumeCost);
            log.debug("[Заказ: {}] Наценка за объём {} м3 (+{}). Промежуточный итог: {}", orderId, volume, volumeCost, cost);
        }

        if (deliveryStreet != null && !deliveryStreet.isEmpty() && !deliveryStreet.equals(warehouseAddress)) {
            BigDecimal addressCost = cost.multiply(ADDRESS_MULTIPLIER);
            cost = cost.add(addressCost);
            log.debug("[Заказ: {}] Наценка за удалённость (+{}). Итоговый расчёт: {}", orderId, addressCost, cost);
        }

        log.info("[Заказ: {}] Расчёт завершён. Итоговая сумма: {}", orderId, cost);
        return cost;
    }

    private void updateDeliveryCostByOrderId(UUID orderId, BigDecimal deliveryCost) {
        log.info("Обновление стоимости доставки для заказа: {} на сумму {}", orderId, deliveryCost);

        DeliveryEntity delivery = getDeliveryByOrderIdEntity(orderId);
        delivery.setDeliveryCost(deliveryCost);

        deliveryRepository.save(delivery);
        log.info("Стоимость доставки обновлена для заказа: {}", orderId);
    }
}