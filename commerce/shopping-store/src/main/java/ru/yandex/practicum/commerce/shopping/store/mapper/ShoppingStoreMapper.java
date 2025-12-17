package ru.yandex.practicum.commerce.shopping.store.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.yandex.practicum.commerce.dto.shopping.store.ProductDto;
import ru.yandex.practicum.commerce.shopping.store.model.ProductEntity;

@Mapper(componentModel = "spring")
public interface ShoppingStoreMapper {

    ProductDto toDto(ProductEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductEntity toEntity(ProductDto dto);

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "productState", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ProductDto dto, @MappingTarget ProductEntity entity);
}