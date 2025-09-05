package com.chessapp.api.account;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping(path = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final String jwtSecret;
    private final AppUserRepository users;

    public AuthController(@Value("${app.security.jwt.secret}") String jwtSecret, AppUserRepository users) {
        this.jwtSecret = jwtSecret;
        this.users = users;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");

        var opt = users.findByUsername(username);
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        if (opt.isEmpty() || !encoder.matches(password, opt.get().getPasswordHash())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        var u = opt.get();

        long now = Instant.now().getEpochSecond();
        long exp = now + 3600;
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = toJson(Map.of(
                "preferred_username", u.getUsername(),
                "roles", u.getRoles(),
                "scope", "api",
                "iat", now,
                "exp", exp
        ));
        String token = signHs256(jwtSecret, headerJson, payloadJson);
        return Map.of("token", token);
    }

    private static String signHs256(String secret, String headerJson, String payloadJson) {
        try {
            String h = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String p = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String signingInput = h + "." + p;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(signingInput.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            String s = Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
            return signingInput + "." + s;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign token", e);
        }
    }

    private static String toJson(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            Object v = e.getValue();
            if (v instanceof String s) {
                sb.append('"').append(escape(s)).append('"');
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(String.valueOf(v));
            } else if (v instanceof java.util.List<?> list) {
                sb.append('[');
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(',');
                    Object li = list.get(i);
                    if (li == null) sb.append("null");
                    else if (li instanceof String ls) sb.append('"').append(escape(ls)).append('"');
                    else sb.append(String.valueOf(li));
                }
                sb.append(']');
            } else if (v == null) {
                sb.append("null");
            } else {
                sb.append('"').append(escape(String.valueOf(v))).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
