package ru.yandex.practicum.commerce.exception;

import org.springframework.http.HttpStatus;

public class NotEnoughInfoInOrderToCalculateException extends ServiceException {
    public NotEnoughInfoInOrderToCalculateException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
