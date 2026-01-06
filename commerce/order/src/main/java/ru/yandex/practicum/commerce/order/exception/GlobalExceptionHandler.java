package ru.yandex.practicum.commerce.order.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import ru.yandex.practicum.commerce.dto.order.exception.NoOrderFoundException;
import ru.yandex.practicum.commerce.dto.shopping.cart.exception.NotAuthorizedUserException;
import ru.yandex.practicum.commerce.dto.warehouse.exception.NoSpecifiedProductInWarehouseException;

import java.util.Collections;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotAuthorizedBusinessException.class)
    public ResponseEntity<NotAuthorizedUserException> handleNotAuthorizedException(
            NotAuthorizedBusinessException ex, WebRequest request) {

        log.warn("Ошибка авторизации: {}", ex.getMessage());

        NotAuthorizedUserException errorResponse = NotAuthorizedUserException.builder()
                .message(ex.getMessage())
                .userMessage("Имя пользователя не должно быть пустым")
                .httpStatus(HttpStatus.UNAUTHORIZED)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .cause(null)
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(NoOrderFoundBusinessException.class)
    public ResponseEntity<NoOrderFoundException> handleNoOrderFoundException(
            NoOrderFoundBusinessException ex, WebRequest request) {

        log.warn("Заказ не найден: {}", ex.getOrderId());

        NoOrderFoundException errorResponse = NoOrderFoundException.builder()
                .message(ex.getMessage())
                .userMessage("Не найден заказ")
                .httpStatus(HttpStatus.BAD_REQUEST)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .cause(null)
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoSpecifiedProductInWarehouseBusinessException.class)
    public ResponseEntity<NoSpecifiedProductInWarehouseException> handleNoProductsInWarehouseException(
            NoSpecifiedProductInWarehouseBusinessException ex, WebRequest request) {

        log.warn("Товары отсутствуют на складе: {}", ex.getProductIds());

        NoSpecifiedProductInWarehouseException errorResponse = NoSpecifiedProductInWarehouseException.builder()
                .message(ex.getMessage())
                .userMessage("Нет заказываемого товара на складе")
                .httpStatus(HttpStatus.BAD_REQUEST)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .cause(null)
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<NotAuthorizedUserException> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Внутренняя ошибка сервера: ", ex);

        NotAuthorizedUserException errorResponse = NotAuthorizedUserException.builder()
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