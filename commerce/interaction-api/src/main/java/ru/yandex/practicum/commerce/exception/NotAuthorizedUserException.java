package ru.yandex.practicum.commerce.exception;

import org.springframework.http.HttpStatus;

public class NotAuthorizedUserException extends ServiceException {

    public NotAuthorizedUserException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
