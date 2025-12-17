package ru.yandex.practicum.commerce.shopping.cart.exceptions;

public class InternalServerErrorFromWarehouseException extends RuntimeException {
    public InternalServerErrorFromWarehouseException(String message) {
        super(message);
    }
}
