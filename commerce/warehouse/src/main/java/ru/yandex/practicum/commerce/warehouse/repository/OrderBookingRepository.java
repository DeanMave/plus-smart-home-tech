package ru.yandex.practicum.commerce.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.commerce.warehouse.model.OrderBookingEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderBookingRepository extends JpaRepository<OrderBookingEntity, UUID> {

    List<OrderBookingEntity> findByOrderId(UUID orderId);

    @Modifying
    @Query("UPDATE OrderBookingEntity ob SET ob.deliveryId = :deliveryId WHERE ob.orderId = :orderId")
    void updateDeliveryIdByOrderId(@Param("orderId") UUID orderId, @Param("deliveryId") UUID deliveryId);

    List<OrderBookingEntity> findAllByProductIdIn(Collection<UUID> productIds);
}
