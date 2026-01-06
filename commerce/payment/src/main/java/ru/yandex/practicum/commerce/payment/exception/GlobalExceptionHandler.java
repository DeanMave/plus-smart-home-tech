package ru.yandex.practicum.commerce.payment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import ru.yandex.practicum.commerce.dto.order.exception.NoOrderFoundException;
import ru.yandex.practicum.commerce.dto.payment.exception.NotEnoughInfoInOrderToCalculateException;

import java.util.Collections;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoOrderFoundBusinessException.class)
    public ResponseEntity<NoOrderFoundException> handleNoOrderFoundException(
            NoOrderFoundBusinessException ex, WebRequest request) {

        log.warn("Заказ не найден: {}", ex.getOrderId());

        NoOrderFoundException errorResponse = NoOrderFoundException.builder()
                .message(ex.getMessage())
                .userMessage("Заказ не найден")
                .httpStatus(HttpStatus.NOT_FOUND)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .cause(null)
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NotEnoughInfoInOrderToCalculateBusinessException.class)
    public ResponseEntity<NotEnoughInfoInOrderToCalculateException> handleNotEnoughInfoException(
            NotEnoughInfoInOrderToCalculateBusinessException ex, WebRequest request) {

        log.warn("Недостаточно информации для расчета заказа: {}", ex.getMessage());

        NotEnoughInfoInOrderToCalculateException errorResponse = NotEnoughInfoInOrderToCalculateException.builder()
                .message(ex.getMessage())
                .userMessage("Недостаточно информации в заказе для расчёта")
                .httpStatus(HttpStatus.BAD_REQUEST)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .cause(null)
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<NoOrderFoundException> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Внутренняя ошибка сервера: ", ex);

        NoOrderFoundException errorResponse = NoOrderFoundException.builder()
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