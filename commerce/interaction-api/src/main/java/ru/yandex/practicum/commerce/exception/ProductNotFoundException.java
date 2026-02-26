package ru.yandex.practicum.commerce.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ServiceException {

    public ProductNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
