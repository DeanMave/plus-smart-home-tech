package ru.yandex.practicum.commerce.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.commerce.warehouse.model.WarehouseProductEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseProductRepository extends JpaRepository<WarehouseProductEntity, UUID> {

    Optional<WarehouseProductEntity> findByProductId(UUID productId);

    List<WarehouseProductEntity> findByProductIdIn(List<UUID> productIds);

    boolean existsByProductId(UUID productId);
}