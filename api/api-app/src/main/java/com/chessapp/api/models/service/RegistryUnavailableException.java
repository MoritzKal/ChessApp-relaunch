package com.chessapp.api.models.service;

public class RegistryUnavailableException extends RuntimeException {
    public RegistryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
    public RegistryUnavailableException(String message) {
        super(message);
    }
}
