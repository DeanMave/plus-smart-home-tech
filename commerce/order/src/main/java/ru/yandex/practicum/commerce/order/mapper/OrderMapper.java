package ru.yandex.practicum.commerce.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.yandex.practicum.commerce.dto.order.OrderDto;
import ru.yandex.practicum.commerce.order.model.OrderEntity;
import ru.yandex.practicum.commerce.order.model.OrderItemEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    @Mapping(target = "state", source = "entity.orderState")
    @Mapping(target = "products", expression = "java(toProductMap(items))")
    OrderDto toDto(OrderEntity entity, List<OrderItemEntity> items);

    default List<OrderItemEntity> toOrderItemEntities(OrderEntity order, Map<UUID, Integer> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        return products.entrySet().stream()
                .map(entry -> OrderItemEntity.builder()
                        .order(order)
                        .productId(entry.getKey())
                        .quantity(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    default Map<UUID, Integer> toProductMap(List<OrderItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return new HashMap<>();
        }
        return items.stream()
                .collect(Collectors.toMap(
                        OrderItemEntity::getProductId,
                        OrderItemEntity::getQuantity
                ));
    }
}