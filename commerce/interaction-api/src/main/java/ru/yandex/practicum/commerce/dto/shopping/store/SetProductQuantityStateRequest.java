package ru.yandex.practicum.commerce.dto.shopping.store;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetProductQuantityStateRequest {
    @NotNull(message = "ID товара не может быть пустым")
    private UUID productId;

    @NotNull(message = "Состояние остатка не может быть пустым")
    private QuantityState quantityState;
}