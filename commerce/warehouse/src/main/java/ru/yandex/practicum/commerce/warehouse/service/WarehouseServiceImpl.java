package ru.yandex.practicum.commerce.warehouse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.shopping.cart.ShoppingCartDto;
import ru.yandex.practicum.commerce.dto.warehouse.AddProductToWarehouseRequest;
import ru.yandex.practicum.commerce.dto.warehouse.AddressDto;
import ru.yandex.practicum.commerce.dto.warehouse.BookedProductsDto;
import ru.yandex.practicum.commerce.dto.warehouse.NewProductInWarehouseRequest;
import ru.yandex.practicum.commerce.warehouse.mapper.WarehouseProductMapper;
import ru.yandex.practicum.commerce.warehouse.repository.WarehouseProductRepository;
import ru.yandex.practicum.commerce.warehouse.exceptions.NoSpecifiedProductInWarehouseBusinessException;
import ru.yandex.practicum.commerce.warehouse.exceptions.ProductInShoppingCartLowQuantityBusinessException;
import ru.yandex.practicum.commerce.warehouse.exceptions.SpecifiedProductAlreadyInWarehouseBusinessException;
import ru.yandex.practicum.commerce.warehouse.model.WarehouseProductEntity;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseProductRepository warehouseProductRepository;
    private final WarehouseProductMapper warehouseMapper;

    private static final String ADDRESS_1 = "ADDRESS_1";
    private static final String ADDRESS_2 = "ADDRESS_2";
    private static final AddressDto ADDRESS_DTO_1 = AddressDto.builder().country(ADDRESS_1).city(ADDRESS_1).street(ADDRESS_1).house(ADDRESS_1).flat(ADDRESS_1).build();
    private static final AddressDto ADDRESS_DTO_2 = AddressDto.builder().country(ADDRESS_2).city(ADDRESS_2).street(ADDRESS_2).house(ADDRESS_2).flat(ADDRESS_2).build();

    private static final List<AddressDto> AVAILABLE_ADDRESSES = List.of(ADDRESS_DTO_1, ADDRESS_DTO_2);

    private static final Random RANDOM = new Random();

    @Override
    public AddressDto getWarehouseAddress() {
        AddressDto address = AVAILABLE_ADDRESSES.get(RANDOM.nextInt(AVAILABLE_ADDRESSES.size()));
        log.debug("Получение адреса склада: {}", address.getCountry());

        return address;
    }

    @Override
    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        log.debug("Добавление нового товара на склад: {}", request.getProductId());

        if (warehouseProductRepository.existsByProductId(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseBusinessException(request.getProductId());
        }

        WarehouseProductEntity entity = warehouseMapper.toEntity(request);
        warehouseProductRepository.save(entity);

        log.info("Новый товар успешно добавлен на склад: {}", request.getProductId());
    }

    @Override
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCartDto) {
        log.debug("Проверка количества товаров для корзины: {}", shoppingCartDto.getShoppingCartId());

        Map<UUID, Integer> requiredProducts = shoppingCartDto.getProducts();

        Set<UUID> productIds = requiredProducts.keySet();

        List<WarehouseProductEntity> productsInWarehouse = warehouseProductRepository.findByProductIdIn(new ArrayList<>(productIds));
        if (productsInWarehouse.size() < productIds.size()) {
            Set<UUID> foundIds = productsInWarehouse.stream()
                    .map(WarehouseProductEntity::getProductId)
                    .collect(Collectors.toSet());

            productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .ifPresent(missingId -> {
                        throw new NoSpecifiedProductInWarehouseBusinessException(missingId);
                    });
        }

        Map<UUID, WarehouseProductEntity> productMap = productsInWarehouse.stream()
                .collect(Collectors.toMap(WarehouseProductEntity::getProductId, Function.identity()));

        Map<UUID, Integer> insufficientProducts = new HashMap<>();
        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean hasFragile = false;

        for (Map.Entry<UUID, Integer> entry : requiredProducts.entrySet()) {
            UUID productId = entry.getKey();
            Integer requiredQuantity = entry.getValue();

            WarehouseProductEntity product = productMap.get(productId);

            if (product.getQuantity() < requiredQuantity) {
                insufficientProducts.put(productId, (int) (requiredQuantity - product.getQuantity()));
            }

            if (product.getWeight() != null) {
                totalWeight += product.getWeight() * requiredQuantity;
            }
            if (product.getWidth() != null && product.getHeight() != null && product.getDepth() != null) {
                totalVolume += product.getWidth() * product.getHeight() * product.getDepth() * requiredQuantity;
            }
            if (Boolean.TRUE.equals(product.getFragile())) {
                hasFragile = true;
            }
        }

        if (!insufficientProducts.isEmpty()) {
            throw new ProductInShoppingCartLowQuantityBusinessException(insufficientProducts);
        }
        BookedProductsDto result = BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(hasFragile)
                .build();

        log.debug("Проверка количества товаров для корзины завершена: {}, результат: {}", shoppingCartDto.getShoppingCartId(), result);
        return result;
    }

    @Override
    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        log.debug("Увеличение количества товара на складе для: {}", request.getProductId());

        WarehouseProductEntity product = warehouseProductRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseBusinessException(request.getProductId()));

        product.setQuantity(product.getQuantity() + request.getQuantity());
        warehouseProductRepository.save(product);

        log.info("Количество товара обновлено для: {}, новое количество: {}", request.getProductId(), product.getQuantity());
    }

}
