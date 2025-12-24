package ru.yandex.practicum.commerce.shopping.cart.config;


import feign.Response;
import feign.codec.ErrorDecoder;
import ru.yandex.practicum.commerce.shopping.cart.exceptions.NoProductsInShoppingCartBusinessException;
import ru.yandex.practicum.commerce.shopping.cart.exceptions.NotAuthorizedBusinessException;

import java.util.Collections;

public class CustomErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        // 401: Пользователь не авторизован
        if (response.status() == 401) {
            return new NotAuthorizedBusinessException("Ошибка авторизации при обращении к складу");
        }

        // 400: Нет товаров или их недостаточно
        if (response.status() == 400) {
            return new NoProductsInShoppingCartBusinessException(Collections.emptyList());
        }

        // Все остальные ошибки (500 и т.д.)
        return defaultDecoder.decode(methodKey, response);
    }
}