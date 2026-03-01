package ru.yandex.practicum.commerce.exception;

import org.springframework.http.HttpStatus;

public class NoOrderFoundException extends ServiceException {
    public NoOrderFoundException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
