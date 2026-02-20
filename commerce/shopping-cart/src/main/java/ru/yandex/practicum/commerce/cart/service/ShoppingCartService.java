package ru.yandex.practicum.commerce.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.cart.model.ShoppingCart;
import ru.yandex.practicum.commerce.cart.repository.ShoppingCartRepository;
import ru.yandex.practicum.commerce.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.commerce.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.commerce.exception.NotAuthorizedUserException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;

    @Transactional
    public ShoppingCartDto getShoppingCart(String username) {
        log.info("Получение корзины для пользователя: {}", username);
        validateUsername(username);
        ShoppingCart cart = getOrCreateActiveCart(username);
        return toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Long> products) {
        log.info("Добавление {} товар(ов) в корзину пользователя: {}", products.size(), username);
        validateUsername(username);
        ShoppingCart cart = getOrCreateActiveCart(username);
        products.forEach((productId, quantity) ->
                cart.getProducts().merge(productId, quantity, Long::sum));
        shoppingCartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        log.info("Деактивация корзины для пользователя: {}", username);
        validateUsername(username);
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
        validateUsername(username);
        ShoppingCart cart = getOrCreateActiveCart(username);

        boolean anyFound = productIds.stream()
                .anyMatch(id -> cart.getProducts().containsKey(id));
        if (!anyFound) {
            throw new NoProductsInShoppingCartException("Указанные товары не найдены в корзине");
        }

        productIds.forEach(id -> cart.getProducts().remove(id));
        shoppingCartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        log.info("Изменение количества товара id={} для пользователя: {}", request.getProductId(), username);
        validateUsername(username);
        ShoppingCart cart = getOrCreateActiveCart(username);

        if (!cart.getProducts().containsKey(request.getProductId())) {
            throw new NoProductsInShoppingCartException("Товар с id=" + request.getProductId() + " не найден в корзине");
        }

        cart.getProducts().put(request.getProductId(), request.getNewQuantity());
        shoppingCartRepository.save(cart);
        return toDto(cart);
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

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("Имя пользователя не должно быть пустым");
        }
    }

    private ShoppingCartDto toDto(ShoppingCart cart) {
        return ShoppingCartDto.builder()
                .shoppingCartId(cart.getId())
                .products(cart.getProducts())
                .build();
    }
}
