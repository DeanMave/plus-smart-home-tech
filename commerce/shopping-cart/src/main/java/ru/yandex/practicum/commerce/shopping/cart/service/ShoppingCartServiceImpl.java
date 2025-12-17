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
import ru.yandex.practicum.commerce.shopping.cart.repository.ShoppingCartItemRepository;
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
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final WarehouseClient warehouseClient;
    private final ShoppingCartMapper shoppingCartMapper;

    @Override
    public ShoppingCartDto getShoppingCart(String username) {
        validateUsername(username);
        log.debug("Получение корзины пользователя: {}", username);

        ShoppingCartEntity shoppingCart = getOrCreateActiveCart(username);
        List<ShoppingCartItemEntity> items = shoppingCartItemRepository
                .findByShoppingCart_ShoppingCartId(shoppingCart.getShoppingCartId());

        return shoppingCartMapper.toDto(shoppingCart, items);
    }

    @Override
    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Integer> productsToAdd) {
        validateUsername(username);
        log.debug("Добавление товаров в корзину для: {}, товары: {}", username, productsToAdd);

        ShoppingCartEntity shoppingCart = getOrCreateActiveCart(username);

        ShoppingCartDto checkRequest = ShoppingCartDto.builder()
                .shoppingCartId(shoppingCart.getShoppingCartId())
                .products(productsToAdd)
                .build();

        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(checkRequest);
            log.debug("Склад подтвердил наличие товаров");
        } catch (Exception e) {
            log.error("Ошибка при проверке наличия на складе: {}", e.getMessage());
            throw e;
        }

        List<ShoppingCartItemEntity> existingItems = shoppingCartItemRepository
                .findByShoppingCart_ShoppingCartIdAndProductIdIn(
                        shoppingCart.getShoppingCartId(),
                        new ArrayList<>(productsToAdd.keySet())
                );

        Map<UUID, ShoppingCartItemEntity> existingItemsMap = existingItems.stream()
                .collect(Collectors.toMap(ShoppingCartItemEntity::getProductId, Function.identity()));

        for (Map.Entry<UUID, Integer> entry : productsToAdd.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantityToAdd = entry.getValue();

            if (existingItemsMap.containsKey(productId)) {
                ShoppingCartItemEntity item = existingItemsMap.get(productId);
                item.setQuantity(item.getQuantity() + quantityToAdd);
                log.debug("Обновлено количество для товара {} в корзине", productId);
            } else {
                ShoppingCartItemEntity newItem = shoppingCartMapper.toNewItemEntity(shoppingCart, productId, quantityToAdd);
                shoppingCartItemRepository.save(newItem);
                log.debug("Добавлен новый товар {} в корзину", productId);
            }
        }

        List<ShoppingCartItemEntity> allItems = shoppingCartItemRepository
                .findByShoppingCart_ShoppingCartId(shoppingCart.getShoppingCartId());

        return shoppingCartMapper.toDto(shoppingCart, allItems);
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
        log.debug("Удаление товаров из корзины пользователя: {}, ID товаров: {}", username, productIds);

        ShoppingCartEntity shoppingCart = getActiveCartOrThrow(username);

        List<ShoppingCartItemEntity> itemsInCart = shoppingCartItemRepository
                .findByShoppingCart_ShoppingCartIdAndProductIdIn(shoppingCart.getShoppingCartId(), productIds);

        if (itemsInCart.size() < productIds.size()) {
            Set<UUID> foundIds = itemsInCart.stream()
                    .map(ShoppingCartItemEntity::getProductId)
                    .collect(Collectors.toSet());

            List<UUID> missingIds = productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());

            throw new NoProductsInShoppingCartBusinessException(missingIds);
        }

        shoppingCartItemRepository.deleteByShoppingCartIdAndProductIds(shoppingCart.getShoppingCartId(), productIds);
        log.debug("Удалено {} товаров из корзины пользователя {}", productIds.size(), username);

        List<ShoppingCartItemEntity> remainingItems = shoppingCartItemRepository
                .findByShoppingCart_ShoppingCartId(shoppingCart.getShoppingCartId());

        return shoppingCartMapper.toDto(shoppingCart, remainingItems);
    }

    @Override
    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        validateUsername(username);
        log.debug("Изменение количества товара в корзине для {}: {}", username, request);

        ShoppingCartEntity shoppingCart = getActiveCartOrThrow(username);

        ShoppingCartItemEntity item = shoppingCartItemRepository
                .findByShoppingCart_ShoppingCartIdAndProductId(shoppingCart.getShoppingCartId(), request.getProductId())
                .orElseThrow(() -> new NoProductsInShoppingCartBusinessException(List.of(request.getProductId())));

        item.setQuantity(request.getNewQuantity().intValue());
        shoppingCartItemRepository.save(item);

        log.debug("Количество товара {} изменено на {}", request.getProductId(), request.getNewQuantity());

        List<ShoppingCartItemEntity> updatedItems = shoppingCartItemRepository
                .findByShoppingCart_ShoppingCartId(shoppingCart.getShoppingCartId());

        return shoppingCartMapper.toDto(shoppingCart, updatedItems);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            log.warn("Попытка доступа с пустым именем пользователя");
            throw new NotAuthorizedBusinessException("Имя пользователя не может быть пустым");
        }
    }

    private ShoppingCartEntity getOrCreateActiveCart(String username) {
        return shoppingCartRepository.findActiveCartByUsername(username)
                .orElseGet(() -> {
                    log.info("Создание новой активной корзины для пользователя: {}", username);
                    return shoppingCartRepository.save(shoppingCartMapper.toNewEntity(username));
                });
    }

    private ShoppingCartEntity getActiveCartOrThrow(String username) {
        return shoppingCartRepository.findActiveCartByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Активная корзина не найдена для пользователя: {}", username);
                    return new NotAuthorizedBusinessException("Активная корзина не найдена для пользователя: " + username);
                });
    }
}