package ru.yandex.practicum.commerce.shopping.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.commerce.shopping.cart.model.ShoppingCartEntity;
import ru.yandex.practicum.commerce.shopping.cart.model.ShoppingCartState;

import java.util.Optional;
import java.util.UUID;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCartEntity, UUID> {

    Optional<ShoppingCartEntity> findByUsernameAndCartState(String username, ShoppingCartState cartState);

    @Query("SELECT sc FROM ShoppingCartEntity sc LEFT JOIN FETCH sc.items WHERE sc.username = :username")
    Optional<ShoppingCartEntity> findByUsernameWithItems(@Param("username") String username);
}
