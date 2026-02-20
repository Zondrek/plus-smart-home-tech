package ru.yandex.practicum.commerce.exception;

import org.springframework.http.HttpStatus;

public class NoSpecifiedProductInWarehouseException extends ServiceException {

    public NoSpecifiedProductInWarehouseException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
