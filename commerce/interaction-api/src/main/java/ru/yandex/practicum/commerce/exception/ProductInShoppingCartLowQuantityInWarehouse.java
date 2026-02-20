package ru.yandex.practicum.commerce.exception;

import org.springframework.http.HttpStatus;

public class ProductInShoppingCartLowQuantityInWarehouse extends ServiceException {

    public ProductInShoppingCartLowQuantityInWarehouse(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
