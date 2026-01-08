package ru.yandex.practicum.commerce.contract.warehouse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.commerce.dto.shopping.cart.ShoppingCartDto;
import ru.yandex.practicum.commerce.dto.warehouse.*;

import java.util.Map;
import java.util.UUID;

public interface WarehouseOperations {

    void newProductInWarehouse(@Valid @RequestBody NewProductInWarehouseRequest request);

    BookedProductsDto checkProductQuantityEnoughForShoppingCart(@Valid @RequestBody ShoppingCartDto shoppingCartDto);

    void addProductToWarehouse(@Valid @RequestBody AddProductToWarehouseRequest request);

    AddressDto getWarehouseAddress();

    void shippedToDelivery(ShippedToDeliveryRequest request);

    void acceptReturn(Map<UUID, Integer> returnedProducts);

    BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request);
}