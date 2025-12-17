package ru.yandex.practicum.commerce.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.commerce.warehouse.model.WarehouseProductEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseProductRepository extends JpaRepository<WarehouseProductEntity, UUID> {

    Optional<WarehouseProductEntity> findByProductId(UUID productId);

    List<WarehouseProductEntity> findByProductIdIn(List<UUID> productIds);

    List<WarehouseProductEntity> findByProductIdInAndQuantityGreaterThan(List<UUID> productIds, Long quantity);

    boolean existsByProductId(UUID productId);
}