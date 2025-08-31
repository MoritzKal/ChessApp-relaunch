package com.chessapp.api.models.service;

public class ModelNotFoundException extends RuntimeException {
    public ModelNotFoundException(String id) {
        super("Model not found: " + id);
    }
}
