package ru.yandex.practicum.commerce.cart.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.commerce.cart.service.ShoppingCartService;
import ru.yandex.practicum.commerce.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.commerce.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.feign.ShoppingCartClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class ShoppingCartController implements ShoppingCartClient {

    private final ShoppingCartService shoppingCartService;

    @Override
    public ShoppingCartDto getShoppingCart(@RequestParam @NotBlank String username) {
        return shoppingCartService.getShoppingCart(username);
    }

    @Override
    public ShoppingCartDto addProductToShoppingCart(@RequestParam @NotBlank String username,
                                                   @RequestBody @NotEmpty Map<UUID, Long> products) {
        return shoppingCartService.addProductToShoppingCart(username, products);
    }

    @Override
    public void deactivateCurrentShoppingCart(@RequestParam @NotBlank String username) {
        shoppingCartService.deactivateCurrentShoppingCart(username);
    }

    @Override
    public ShoppingCartDto removeFromShoppingCart(@RequestParam @NotBlank String username,
                                                 @RequestBody @NotEmpty List<UUID> productIds) {
        return shoppingCartService.removeFromShoppingCart(username, productIds);
    }

    @Override
    public ShoppingCartDto changeProductQuantity(@RequestParam @NotBlank String username,
                                                @RequestBody @Valid ChangeProductQuantityRequest request) {
        return shoppingCartService.changeProductQuantity(username, request);
    }
}
