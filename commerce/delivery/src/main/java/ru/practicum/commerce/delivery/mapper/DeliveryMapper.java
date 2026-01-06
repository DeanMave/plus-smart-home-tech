package ru.practicum.commerce.delivery.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.practicum.commerce.delivery.model.DeliveryEntity;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryDto;
import ru.yandex.practicum.commerce.dto.warehouse.AddressDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DeliveryMapper {
    DeliveryDto toDto(DeliveryEntity entity);

    DeliveryEntity toEntity(DeliveryDto dto);

    DeliveryEntity.Address map(AddressDto dto);

    AddressDto map(DeliveryEntity.Address address);
}