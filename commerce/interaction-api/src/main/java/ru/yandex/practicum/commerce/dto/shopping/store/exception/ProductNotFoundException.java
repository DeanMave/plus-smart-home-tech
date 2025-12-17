package ru.yandex.practicum.commerce.dto.shopping.store.exception;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.yandex.practicum.commerce.dto.exception.AbstractErrorResponse;

@SuperBuilder
@NoArgsConstructor
public class ProductNotFoundException extends AbstractErrorResponse {
}
