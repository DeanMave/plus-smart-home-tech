package ru.yandex.practicum.commerce.shopping.cart.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import ru.yandex.practicum.commerce.dto.shopping.cart.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.commerce.dto.shopping.cart.exception.NotAuthorizedUserException;

import java.util.Collections;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotAuthorizedBusinessException.class)
    public ResponseEntity<NotAuthorizedUserException> handleNotAuthorizedException(
            NotAuthorizedBusinessException ex, WebRequest request) {

        log.warn("Пользователь не авторизован: {}", ex.getMessage());

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

    @ExceptionHandler(NoProductsInShoppingCartBusinessException.class)
    public ResponseEntity<NoProductsInShoppingCartException> handleNoProductsException(
            NoProductsInShoppingCartBusinessException ex, WebRequest request) {

        log.warn("Товары не найдены в корзине: {}", ex.getProductIds());

        NoProductsInShoppingCartException errorResponse = NoProductsInShoppingCartException.builder()
                .message(ex.getMessage())
                .userMessage("Нет искомых товаров в корзине")
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
                .message("Internal server error")
                .userMessage("Произошла внутренняя ошибка сервера")
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}