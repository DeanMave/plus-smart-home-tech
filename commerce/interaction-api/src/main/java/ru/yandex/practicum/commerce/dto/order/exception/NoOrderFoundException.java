package ru.yandex.practicum.commerce.dto.order.exception;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.yandex.practicum.commerce.dto.exception.AbstractErrorResponse;

@SuperBuilder
@NoArgsConstructor
public class NoOrderFoundException extends AbstractErrorResponse {
}
