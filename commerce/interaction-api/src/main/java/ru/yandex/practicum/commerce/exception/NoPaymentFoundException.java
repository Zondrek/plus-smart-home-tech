package ru.yandex.practicum.commerce.exception;

import org.springframework.http.HttpStatus;

public class NoPaymentFoundException extends ServiceException {
    public NoPaymentFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
