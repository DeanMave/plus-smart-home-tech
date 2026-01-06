package ru.yandex.practicum.commerce.warehouse.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.contract.warehouse.WarehouseOperations;
import ru.yandex.practicum.commerce.dto.shopping.cart.ShoppingCartDto;
import ru.yandex.practicum.commerce.dto.warehouse.*;
import ru.yandex.practicum.commerce.warehouse.service.WarehouseService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController implements WarehouseOperations {

    private final WarehouseService warehouseService;

    @Override
    @PutMapping
    public void newProductInWarehouse(@RequestBody NewProductInWarehouseRequest request) {
        log.debug("Добавление нового товара на склад: {}", request.getProductId());
        warehouseService.newProductInWarehouse(request);
        log.debug("Новый товар успешно добавлен");
    }

    @Override
    @PostMapping("/check")
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(@RequestBody ShoppingCartDto shoppingCartDto) {
        log.debug("Проверка наличия товаров для корзины: {}", shoppingCartDto.getShoppingCartId());
        BookedProductsDto result = warehouseService.checkProductQuantityEnoughForShoppingCart(shoppingCartDto);
        log.debug("Результат проверки: {}", result);
        return result;
    }

    @Override
    @PostMapping("/add")
    public void addProductToWarehouse(@RequestBody AddProductToWarehouseRequest request) {
        log.debug("Увеличение количества товара на складе: {}", request.getProductId());
        warehouseService.addProductToWarehouse(request);
        log.debug("Количество товара успешно увеличено");
    }

    @Override
    @GetMapping("/address")
    public AddressDto getWarehouseAddress() {
        log.debug("Запрос адреса склада");
        AddressDto address = warehouseService.getWarehouseAddress();
        log.debug("Получен адрес склада: {}", address.getCountry());
        return address;
    }

    @Override
    @PostMapping("/assembly")
    public BookedProductsDto assemblyProductsForOrder(@RequestBody AssemblyProductsForOrderRequest request) {
        log.debug("Запуск сборки товаров для заказа: {}", request.getOrderId());
        BookedProductsDto result = warehouseService.assemblyProductsForOrder(request);
        log.debug("Сборка заказа {} завершена", request.getOrderId());
        return result;
    }

    @Override
    @PostMapping("/shipped")
    public void shippedToDelivery(@RequestBody ShippedToDeliveryRequest request) {
        log.debug("Отгрузка заказа {} в доставку (ID доставки: {})", request.getOrderId(), request.getDeliveryId());
        warehouseService.shippedToDelivery(request);
        log.debug("Заказ {} успешно отгружен", request.getOrderId());
    }

    @Override
    @PostMapping("/return")
    public void acceptReturn(@RequestBody Map<UUID, Integer> returnedProducts) {
        log.debug("Прием возврата товаров: {}", returnedProducts.keySet());
        warehouseService.acceptReturn(returnedProducts);
        log.debug("Возврат успешно принят");
    }
}