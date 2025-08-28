package com.chessapp.api.common;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Validation failed"
        ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = ex.getParameterName() + " is required";
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Bad Request",
                "message", message
        ));
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Map<String, Object>> handleBinding(ServletRequestBindingException ex) {
        String message = ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Bad Request",
                "message", message != null ? message : "Bad request"
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus effective = (status != null) ? status : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(effective).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", effective.value(),
                "error", effective.getReasonPhrase(),
                "message", ex.getReason() != null ? ex.getReason() : "Bad request"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        if (ex instanceof MissingServletRequestParameterException) {
            return handleMissingParam((MissingServletRequestParameterException) ex);
        }
        if (ex instanceof ServletRequestBindingException) {
            return handleBinding((ServletRequestBindingException) ex);
        }
        if (ex instanceof ResponseStatusException rse) {
            return handleResponseStatus(rse);
        }
        return ResponseEntity.internalServerError().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error", "Internal server error"
        ));
    }
}
