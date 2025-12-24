package ru.yandex.practicum.commerce.dto.warehouse.exception;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.yandex.practicum.commerce.dto.exception.AbstractErrorResponse;


@SuperBuilder
@NoArgsConstructor
public class SpecifiedProductAlreadyInWarehouseException extends AbstractErrorResponse {
}
