package ru.yandex.practicum.commerce.shopping.cart.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.yandex.practicum.commerce.dto.shopping.cart.ShoppingCartDto;
import ru.yandex.practicum.commerce.shopping.cart.model.ShoppingCartEntity;
import ru.yandex.practicum.commerce.shopping.cart.model.ShoppingCartItemEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ShoppingCartMapper {

    @Mapping(target = "products", source = "items", qualifiedByName = "mapItemsToMap")
    ShoppingCartDto toDto(ShoppingCartEntity shoppingCart, List<ShoppingCartItemEntity> items);

    @Named("mapItemsToMap")
    default Map<UUID, Integer> mapItemsToMap(List<ShoppingCartItemEntity> items) {
        if (items == null) return Collections.emptyMap();
        return items.stream()
                .collect(Collectors.toMap(
                        ShoppingCartItemEntity::getProductId,
                        ShoppingCartItemEntity::getQuantity
                ));
    }

    @Mapping(target = "cartState", constant = "ACTIVE")
    ShoppingCartEntity toNewEntity(String username);

    @Mapping(target = "cartItemId", ignore = true)
    @Mapping(target = "shoppingCart", source = "shoppingCart")
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "quantity", source = "quantity")
    ShoppingCartItemEntity toNewItemEntity(ShoppingCartEntity shoppingCart, UUID productId, Integer quantity);
}