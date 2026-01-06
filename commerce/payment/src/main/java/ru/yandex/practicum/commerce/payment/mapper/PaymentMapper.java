package ru.yandex.practicum.commerce.payment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.yandex.practicum.commerce.dto.payment.PaymentDto;
import ru.yandex.practicum.commerce.payment.model.PaymentEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

    @Mapping(target = "totalPayment", source = "totalCost")
    @Mapping(target = "deliveryTotal", source = "deliveryCost")
    @Mapping(target = "feeTotal", source = "taxCost")
    PaymentDto toDto(PaymentEntity entity);

    default PaymentEntity createNewPayment(UUID orderId, BigDecimal productCost,
                                           BigDecimal deliveryCost, BigDecimal taxCost,
                                           BigDecimal totalCost) {
        return PaymentEntity.builder()
                .orderId(orderId)
                .productCost(productCost)
                .deliveryCost(deliveryCost)
                .taxCost(taxCost)
                .totalCost(totalCost)
                .build();
    }
}
