package ru.yandex.practicum.commerce.warehouse.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.commerce.dto.warehouse.DimensionDto;
import ru.yandex.practicum.commerce.dto.warehouse.NewProductInWarehouseRequest;
import ru.yandex.practicum.commerce.warehouse.model.WarehouseProductEntity;

@Mapper(componentModel = "spring")
public interface WarehouseProductMapper {

    @Mapping(target = "quantity", constant = "0L")
    @Mapping(target = "width", source = "dimension.width")
    @Mapping(target = "height", source = "dimension.height")
    @Mapping(target = "depth", source = "dimension.depth")
    WarehouseProductEntity toEntity(NewProductInWarehouseRequest request);

    DimensionDto toDimensionDto(WarehouseProductEntity entity);
}