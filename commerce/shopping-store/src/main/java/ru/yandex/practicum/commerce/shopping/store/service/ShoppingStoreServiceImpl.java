package ru.yandex.practicum.commerce.shopping.store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.shopping.store.ProductCategory;
import ru.yandex.practicum.commerce.dto.shopping.store.ProductDto;
import ru.yandex.practicum.commerce.dto.shopping.store.ProductState;
import ru.yandex.practicum.commerce.dto.shopping.store.SetProductQuantityStateRequest;
import ru.yandex.practicum.commerce.shopping.store.repository.ProductRepository;
import ru.yandex.practicum.commerce.shopping.store.exceptions.ProductNotFoundBusinessException;
import ru.yandex.practicum.commerce.shopping.store.mapper.ShoppingStoreMapper;
import ru.yandex.practicum.commerce.shopping.store.model.ProductEntity;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShoppingStoreServiceImpl implements ShoppingStoreService {

    private final ProductRepository productRepository;
    private final ShoppingStoreMapper productMapper;

    @Override
    public Page<ProductDto> getProducts(ProductCategory category, int page, int size, String sort) {
        Pageable pageable = createPageable(page, size, sort);
        Page<ProductEntity> productPage = productRepository.findByProductCategory(category, pageable);

        return productPage.map(productMapper::toDto);
    }

    @Override
    @Transactional
    public ProductDto createProduct(ProductDto productDto) {
        log.debug("Создание нового товара: {}", productDto.getProductName());
        ProductEntity entity = productMapper.toEntity(productDto);

        if (entity.getProductState() == null) {
            entity.setProductState(ProductState.ACTIVE);
        }

        ProductEntity saved = productRepository.save(entity);
        return productMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
        log.debug("Обновление товара с ID: {}", productDto.getProductId());
        ProductEntity existingProduct = findEntityOrThrow(productDto.getProductId());

        productMapper.updateEntityFromDto(productDto, existingProduct);
        return productMapper.toDto(productRepository.save(existingProduct));
    }

    @Override
    @Transactional
    public boolean removeProductFromStore(UUID productId) {
        ProductEntity product = findEntityOrThrow(productId);
        product.setProductState(ProductState.DEACTIVATE);
        productRepository.save(product);
        return true;
    }

    @Override
    @Transactional
    public boolean setProductQuantityState(SetProductQuantityStateRequest request) {
        ProductEntity product = findEntityOrThrow(request.getProductId());
        product.setQuantityState(request.getQuantityState());
        productRepository.save(product);
        return true;
    }

    @Override
    public ProductDto getProduct(UUID productId) {
        return productMapper.toDto(findEntityOrThrow(productId));
    }

    private ProductEntity findEntityOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundBusinessException(productId));
    }

    private Pageable createPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }
        String[] parts = sort.split(",");
        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, parts[0].trim()));
    }
}