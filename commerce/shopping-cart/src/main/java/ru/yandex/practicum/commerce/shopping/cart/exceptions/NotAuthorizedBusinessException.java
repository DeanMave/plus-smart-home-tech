package ru.yandex.practicum.commerce.shopping.cart.exceptions;

public class NotAuthorizedBusinessException extends RuntimeException {

    public NotAuthorizedBusinessException(String message) {
        super(message);
    }

    public NotAuthorizedBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
