package ru.practicum.commerce.delivery.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import ru.yandex.practicum.commerce.dto.delivery.exception.NoDeliveryFoundException;

import java.util.Collections;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoDeliveryFoundBusinessException.class)
    public ResponseEntity<NoDeliveryFoundException> handleNoDeliveryFoundException(
            NoDeliveryFoundBusinessException ex, WebRequest request) {

        log.warn("Доставка не найдена: {}", ex.getDeliveryId());

        NoDeliveryFoundException errorResponse = NoDeliveryFoundException.builder()
                .message(ex.getMessage())
                .userMessage("Доставка не найдена")
                .httpStatus(HttpStatus.NOT_FOUND)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .cause(null)
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<NoDeliveryFoundException> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Внутренняя ошибка сервера доставки: ", ex);

        NoDeliveryFoundException errorResponse = NoDeliveryFoundException.builder()
                .message("Internal server error: " + ex.getClass().getName())
                .userMessage("Произошла внутренняя ошибка сервера")
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .cause(null)
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}