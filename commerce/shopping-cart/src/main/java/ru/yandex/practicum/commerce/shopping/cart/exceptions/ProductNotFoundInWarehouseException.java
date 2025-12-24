package ru.yandex.practicum.commerce.shopping.cart.exceptions;

public class ProductNotFoundInWarehouseException extends RuntimeException {
    public ProductNotFoundInWarehouseException(String message) {
        super(message);
    }
}
