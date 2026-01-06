package ru.yandex.practicum.commerce.dto.payment.exception;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.yandex.practicum.commerce.dto.exception.AbstractErrorResponse;

@SuperBuilder
@NoArgsConstructor
public class NotEnoughInfoInOrderToCalculateException extends AbstractErrorResponse {
}
