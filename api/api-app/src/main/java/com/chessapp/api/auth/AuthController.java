package com.chessapp.api.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chessapp.api.security.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final String adminUsername;
    private final String adminPassword;

    public AuthController(JwtService jwtService,
                          @Value("${ADMIN_USERNAME:admin}") String adminUsername,
                          @Value("${ADMIN_PASSWORD:password}") String adminPassword) {
        this.jwtService = jwtService;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> login(@RequestBody LoginRequest request) {
        List<String> roles = new ArrayList<>();
        roles.add("USER");
        if (adminUsername.equals(request.username()) && adminPassword.equals(request.password())) {
            roles.add("ADMIN");
        }
        String token = jwtService.generateToken(request.username(), roles);
        return Map.of("token", token);
    }

    public record LoginRequest(String username, String password) {
    }
}
