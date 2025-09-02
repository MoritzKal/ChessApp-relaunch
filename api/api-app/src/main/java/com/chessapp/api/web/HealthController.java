package com.chessapp.api.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/v1")
public class HealthController {

    @GetMapping("/health")
    @Operation(security = {})
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
