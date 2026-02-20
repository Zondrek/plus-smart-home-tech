package ru.yandex.practicum.commerce.exception;

import org.springframework.http.HttpStatus;

public class SpecifiedProductAlreadyInWarehouseException extends ServiceException {

    public SpecifiedProductAlreadyInWarehouseException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
