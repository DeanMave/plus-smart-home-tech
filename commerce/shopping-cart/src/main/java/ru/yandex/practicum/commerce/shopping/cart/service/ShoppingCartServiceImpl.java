package ru.yandex.practicum.commerce.shopping.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.contract.warehouse.WarehouseClient;
import ru.yandex.practicum.commerce.dto.shopping.cart.ChangeProductQuantityRequest;
import ru.yandex.practicum.commerce.dto.shopping.cart.ShoppingCartDto;
import ru.yandex.practicum.commerce.shopping.cart.exceptions.NoProductsInShoppingCartBusinessException;
import ru.yandex.practicum.commerce.shopping.cart.exceptions.NotAuthorizedBusinessException;
import ru.yandex.practicum.commerce.shopping.cart.mapper.ShoppingCartMapper;
import ru.yandex.practicum.commerce.shopping.cart.model.ShoppingCartEntity;
import ru.yandex.practicum.commerce.shopping.cart.model.ShoppingCartItemEntity;
import ru.yandex.practicum.commerce.shopping.cart.model.ShoppingCartState;
import ru.yandex.practicum.commerce.shopping.cart.repository.ShoppingCartRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final WarehouseClient warehouseClient;
    private final ShoppingCartMapper shoppingCartMapper;

    @Override
    public ShoppingCartDto getShoppingCart(String username) {
        validateUsername(username);
        ShoppingCartEntity shoppingCart = getOrCreateActiveCartWithItems(username);
        return shoppingCartMapper.toDto(shoppingCart, shoppingCart.getItems());
    }

    @Override
    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Integer> productsToAdd) {
        validateUsername(username);
        log.debug("Добавление товаров для: {}, список: {}", username, productsToAdd);

        ShoppingCartEntity shoppingCart = getOrCreateActiveCartWithItems(username);

        ShoppingCartDto checkRequest = ShoppingCartDto.builder()
                .shoppingCartId(shoppingCart.getShoppingCartId())
                .products(productsToAdd)
                .build();

        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(checkRequest);
        } catch (Exception e) {
            log.error("Склад отклонил запрос: {}", e.getMessage());
            throw e;
        }

        Map<UUID, ShoppingCartItemEntity> existingItemsMap = shoppingCart.getItems().stream()
                .collect(Collectors.toMap(ShoppingCartItemEntity::getProductId, Function.identity()));

        for (Map.Entry<UUID, Integer> entry : productsToAdd.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantityToAdd = entry.getValue();

            if (existingItemsMap.containsKey(productId)) {
                ShoppingCartItemEntity item = existingItemsMap.get(productId);
                item.setQuantity(item.getQuantity() + quantityToAdd);
                log.debug("Обновлено количество товара {} (+{})", productId, quantityToAdd);
            } else {
                ShoppingCartItemEntity newItem = new ShoppingCartItemEntity();
                newItem.setProductId(productId);
                newItem.setQuantity(quantityToAdd);

                shoppingCart.addItem(newItem);

                log.debug("Добавлен новый товар {}", productId);
            }
        }

        shoppingCartRepository.save(shoppingCart);

        return shoppingCartMapper.toDto(shoppingCart, shoppingCart.getItems());
    }

    @Override
    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        validateUsername(username);
        log.debug("Деактивация корзины для пользователя: {}", username);

        shoppingCartRepository.findByUsernameAndCartState(username, ShoppingCartState.ACTIVE)
                .ifPresentOrElse(cart -> {
                    cart.setCartState(ShoppingCartState.DEACTIVATED);
                    shoppingCartRepository.save(cart);
                    log.info("Корзина пользователя {} успешно деактивирована", username);
                }, () -> {
                    log.warn("Активная корзина для пользователя {} не найдена", username);
                });
    }

    @Override
    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
        validateUsername(username);
        log.debug("Удаление товаров из корзины пользователя: {}", username);

        ShoppingCartEntity shoppingCart = getOrCreateActiveCartWithItems(username);

        Set<UUID> productIdsInCart = shoppingCart.getItems().stream()
                .map(ShoppingCartItemEntity::getProductId)
                .collect(Collectors.toSet());

        List<UUID> missingIds = productIds.stream()
                .filter(id -> !productIdsInCart.contains(id))
                .collect(Collectors.toList());

        if (!missingIds.isEmpty()) {
            throw new NoProductsInShoppingCartBusinessException(missingIds);
        }

        shoppingCart.getItems().removeIf(item -> productIds.contains(item.getProductId()));
        log.debug("Удалено {} товаров", productIds.size());

        shoppingCartRepository.save(shoppingCart);

        return shoppingCartMapper.toDto(shoppingCart, shoppingCart.getItems());
    }

    @Override
    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        validateUsername(username);

        ShoppingCartEntity shoppingCart = getOrCreateActiveCartWithItems(username);

        ShoppingCartItemEntity item = shoppingCart.getItems().stream()
                .filter(i -> i.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElseThrow(() -> new NoProductsInShoppingCartBusinessException(List.of(request.getProductId())));

        item.setQuantity(request.getNewQuantity().intValue());

        shoppingCartRepository.save(shoppingCart);

        return shoppingCartMapper.toDto(shoppingCart, shoppingCart.getItems());
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            log.warn("Попытка доступа с пустым именем пользователя");
            throw new NotAuthorizedBusinessException("Имя пользователя не может быть пустым");
        }
    }

    private ShoppingCartEntity getOrCreateActiveCartWithItems(String username) {
        return shoppingCartRepository.findByUsernameWithItems(username)
                .orElseGet(() -> {
                    ShoppingCartEntity newCart = new ShoppingCartEntity();
                    newCart.setUsername(username);
                    return shoppingCartRepository.save(newCart);
                });
    }
}