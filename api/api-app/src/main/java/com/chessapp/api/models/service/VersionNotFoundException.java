package com.chessapp.api.models.service;

public class VersionNotFoundException extends RuntimeException {
    public VersionNotFoundException(String id, String ver) {
        super("Version not found: " + ver + " for model: " + id);
    }
}
