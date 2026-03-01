package ru.yandex.practicum.commerce.delivery.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.commerce.exception.ServiceException;

import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Map<String, String>> handleServiceException(ServiceException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(Map.of(
                        "httpStatus", ex.getHttpStatus().name(),
                        "userMessage", ex.getUserMessage()
                ));
    }
}
