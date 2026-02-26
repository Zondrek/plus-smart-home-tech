package ru.yandex.practicum.commerce.store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.ProductCategory;
import ru.yandex.practicum.commerce.dto.ProductDto;
import ru.yandex.practicum.commerce.dto.ProductState;
import ru.yandex.practicum.commerce.dto.QuantityState;
import ru.yandex.practicum.commerce.exception.ProductNotFoundException;
import ru.yandex.practicum.commerce.store.mapper.ProductMapper;
import ru.yandex.practicum.commerce.store.model.Product;
import ru.yandex.practicum.commerce.store.repository.ProductRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingStoreService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(ProductCategory category, Pageable pageable) {
        log.info("Получение товаров по категории: {}", category);
        return productRepository.findByProductCategory(category, pageable)
                .map(productMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID productId) {
        log.info("Получение товара с id={}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Товар с id=" + productId + " не найден"));
        return productMapper.toDto(product);
    }

    @Transactional
    public ProductDto createNewProduct(ProductDto productDto) {
        log.info("Создание нового товара: {}", productDto.getProductName());
        Product product = productMapper.toEntity(productDto);
        Product saved = productRepository.save(product);
        log.info("Товар создан с id={}", saved.getId());
        return productMapper.toDto(saved);
    }

    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
        log.info("Обновление товара с id={}", productDto.getProductId());
        Product existing = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Товар с id=" + productDto.getProductId() + " не найден"));

        if (productDto.getProductName() != null) {
            existing.setProductName(productDto.getProductName());
        }
        if (productDto.getDescription() != null) {
            existing.setDescription(productDto.getDescription());
        }
        if (productDto.getImageSrc() != null) {
            existing.setImageSrc(productDto.getImageSrc());
        }
        if (productDto.getQuantityState() != null) {
            existing.setQuantityState(productDto.getQuantityState());
        }
        if (productDto.getProductState() != null) {
            existing.setProductState(productDto.getProductState());
        }
        if (productDto.getProductCategory() != null) {
            existing.setProductCategory(productDto.getProductCategory());
        }
        if (productDto.getPrice() != null) {
            existing.setPrice(productDto.getPrice());
        }

        Product saved = productRepository.save(existing);
        log.info("Товар с id={} обновлён", saved.getId());
        return productMapper.toDto(saved);
    }

    @Transactional
    public boolean removeProductFromStore(UUID productId) {
        log.info("Удаление товара из магазина: id={}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Товар с id=" + productId + " не найден"));
        product.setProductState(ProductState.DEACTIVATE);
        productRepository.save(product);
        log.info("Товар с id={} деактивирован", productId);
        return true;
    }

    @Transactional
    public boolean setProductQuantityState(UUID productId, QuantityState quantityState) {
        log.info("Установка состояния количества для товара id={}: {}", productId, quantityState);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Товар с id=" + productId + " не найден"));
        product.setQuantityState(quantityState);
        productRepository.save(product);
        return true;
    }
}
