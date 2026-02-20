package ru.yandex.practicum.commerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ServiceException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String userMessage;

    protected ServiceException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.userMessage = message;
    }
}
