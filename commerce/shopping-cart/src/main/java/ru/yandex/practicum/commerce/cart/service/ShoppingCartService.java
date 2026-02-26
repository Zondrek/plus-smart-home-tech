package ru.yandex.practicum.commerce.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.cart.mapper.ShoppingCartMapper;
import ru.yandex.practicum.commerce.cart.model.ShoppingCart;
import ru.yandex.practicum.commerce.cart.repository.ShoppingCartRepository;
import ru.yandex.practicum.commerce.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.commerce.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.commerce.feign.WarehouseClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartMapper shoppingCartMapper;
    private final WarehouseClient warehouseClient;

    @Transactional
    public ShoppingCartDto getShoppingCart(String username) {
        log.info("Получение корзины для пользователя: {}", username);
        ShoppingCart cart = getOrCreateActiveCart(username);
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Long> products) {
        log.info("Добавление {} товар(ов) в корзину пользователя: {}", products.size(), username);
        ShoppingCart cart = getOrCreateActiveCart(username);
        products.forEach((productId, quantity) ->
                cart.getProducts().merge(productId, quantity, Long::sum));

        ShoppingCartDto cartDto = shoppingCartMapper.toDto(cart);
        warehouseClient.checkProductQuantityEnoughForShoppingCart(cartDto);

        shoppingCartRepository.save(cart);
        return cartDto;
    }

    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        log.info("Деактивация корзины для пользователя: {}", username);
        shoppingCartRepository.findByUsernameAndActiveTrue(username)
                .ifPresent(cart -> {
                    cart.setActive(false);
                    shoppingCartRepository.save(cart);
                    log.info("Корзина id={} деактивирована", cart.getId());
                });
    }

    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
        log.info("Удаление {} товар(ов) из корзины пользователя: {}", productIds.size(), username);
        ShoppingCart cart = getOrCreateActiveCart(username);

        boolean anyFound = productIds.stream()
                .anyMatch(id -> cart.getProducts().containsKey(id));
        if (!anyFound) {
            throw new NoProductsInShoppingCartException("Указанные товары не найдены в корзине");
        }

        productIds.forEach(id -> cart.getProducts().remove(id));
        shoppingCartRepository.save(cart);
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        log.info("Изменение количества товара id={} для пользователя: {}", request.getProductId(), username);
        ShoppingCart cart = getOrCreateActiveCart(username);

        if (!cart.getProducts().containsKey(request.getProductId())) {
            throw new NoProductsInShoppingCartException("Товар с id=" + request.getProductId() + " не найден в корзине");
        }

        cart.getProducts().put(request.getProductId(), request.getNewQuantity());

        ShoppingCartDto cartDto = shoppingCartMapper.toDto(cart);
        warehouseClient.checkProductQuantityEnoughForShoppingCart(cartDto);

        shoppingCartRepository.save(cart);
        return cartDto;
    }

    private ShoppingCart getOrCreateActiveCart(String username) {
        return shoppingCartRepository.findByUsernameAndActiveTrue(username)
                .orElseGet(() -> {
                    log.info("Создание новой корзины для пользователя: {}", username);
                    ShoppingCart newCart = ShoppingCart.builder()
                            .username(username)
                            .active(true)
                            .build();
                    return shoppingCartRepository.save(newCart);
                });
    }
}
