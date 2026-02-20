package ru.yandex.practicum.commerce.exception;

import org.springframework.http.HttpStatus;

public class NoProductsInShoppingCartException extends ServiceException {

    public NoProductsInShoppingCartException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
