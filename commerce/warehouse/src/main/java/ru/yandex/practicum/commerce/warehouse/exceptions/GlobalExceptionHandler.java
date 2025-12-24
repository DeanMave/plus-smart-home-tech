package ru.yandex.practicum.commerce.warehouse.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import ru.yandex.practicum.commerce.dto.warehouse.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.commerce.dto.warehouse.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.commerce.dto.warehouse.exception.SpecifiedProductAlreadyInWarehouseException;

import java.util.Collections;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Товар уже зарегистрирован на складе
    @ExceptionHandler(SpecifiedProductAlreadyInWarehouseBusinessException.class)
    public ResponseEntity<SpecifiedProductAlreadyInWarehouseException> handleProductAlreadyInWarehouse(
            SpecifiedProductAlreadyInWarehouseBusinessException ex, WebRequest request) {

        log.warn("Product already in warehouse: {}", ex.getProductId());

        SpecifiedProductAlreadyInWarehouseException errorResponse = SpecifiedProductAlreadyInWarehouseException.builder()
                .message(ex.getMessage())
                .userMessage("Ошибка, товар с таким описанием уже зарегистрирован на складе")
                .httpStatus(HttpStatus.BAD_REQUEST)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .cause(null)
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Недостаточное количество товара на складе
    @ExceptionHandler(ProductInShoppingCartLowQuantityBusinessException.class)
    public ResponseEntity<ProductInShoppingCartLowQuantityInWarehouse> handleLowQuantity(
            ProductInShoppingCartLowQuantityBusinessException ex, WebRequest request) {

        log.warn("Insufficient products in warehouse: {}", ex.getInsufficientProducts());

        ProductInShoppingCartLowQuantityInWarehouse errorResponse = ProductInShoppingCartLowQuantityInWarehouse.builder()
                .message(ex.getMessage())
                .userMessage("Ошибка, товар из корзины не находится в требуемом количестве на складе")
                .httpStatus(HttpStatus.BAD_REQUEST)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .cause(null)
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Товар не найден на складе
    @ExceptionHandler(NoSpecifiedProductInWarehouseBusinessException.class)
    public ResponseEntity<NoSpecifiedProductInWarehouseException> handleNoProductInWarehouse(
            NoSpecifiedProductInWarehouseBusinessException ex, WebRequest request) {

        log.warn("Product not found in warehouse: {}", ex.getProductId());

        NoSpecifiedProductInWarehouseException errorResponse = NoSpecifiedProductInWarehouseException.builder()
                .message(ex.getMessage())
                .userMessage("Нет информации о товаре на складе")
                .httpStatus(HttpStatus.BAD_REQUEST)
                .localizedMessage(ex.getLocalizedMessage())
                .stackTrace(Collections.emptyList())
                .cause(null)
                .suppressed(Collections.emptyList())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Общий обработчик: 500 INTERNAL_SERVER_ERROR
    @ExceptionHandler(Exception.class)
    public ResponseEntity<SpecifiedProductAlreadyInWarehouseException> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Internal server error: ", ex);

        SpecifiedProductAlreadyInWarehouseException errorResponse = SpecifiedProductAlreadyInWarehouseException.builder()
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