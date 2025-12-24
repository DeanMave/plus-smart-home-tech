package ru.yandex.practicum.commerce.shopping.store.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import ru.yandex.practicum.commerce.dto.shopping.store.exception.ProductNotFoundException;

import java.util.Collections;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundBusinessException.class)
    public ResponseEntity<ProductNotFoundException> handleProductNotFoundException(
            ProductNotFoundBusinessException ex, WebRequest request) {

        log.warn("Товар не найден: {}", ex.getMessage());

        ProductNotFoundException errorResponse = ProductNotFoundException.builder()
                .message(ex.getMessage())
                .userMessage("Товар не найден в каталоге")
                .httpStatus(HttpStatus.NOT_FOUND)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProductNotFoundException> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Внутренняя ошибка сервера: ", ex);

        ProductNotFoundException errorResponse = ProductNotFoundException.builder()
                .message("Internal server error")
                .userMessage("Произошла внутренняя ошибка сервера")
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}