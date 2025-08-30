package com.chessapp.api.models.api;

import java.util.Collections;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing read-only model registry endpoints.
 */
@RestController
@RequestMapping("/models")
public class ModelsController {

    @GetMapping
    public ResponseEntity<List<?>> listModels(
            @RequestHeader(value = "X-Username", required = false) String username) {
        // Only update MDC when a username is supplied to avoid IllegalArgumentException
        if (username != null) {
            MDC.put("username", username);
        }
        try {
            return ResponseEntity.ok(Collections.emptyList());
        } finally {
            MDC.clear();
        }
    }
}

