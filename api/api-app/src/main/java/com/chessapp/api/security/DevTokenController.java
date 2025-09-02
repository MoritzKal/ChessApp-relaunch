package com.chessapp.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Dev helper endpoint to mint HS256 JWTs with the configured secret.
 * Enable/disable via property: app.security.dev-token.enabled (default: true).
 * IMPORTANT: Disable in production environments.
 */
@RestController
@RequestMapping(path = "/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "app.security.dev-token.enabled", havingValue = "true", matchIfMissing = false)
public class DevTokenController {

    private final String jwtSecret;

    public DevTokenController(@Value("${app.security.jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @GetMapping("/token")
    @Operation(summary = "Mint dev token", security = {})
    public Map<String, Object> mintToken(
            @RequestParam(name = "user", defaultValue = "user1") String user,
            @RequestParam(name = "roles", defaultValue = "USER") String rolesCsv,
            @RequestParam(name = "scope", defaultValue = "api.read") String scope,
            @RequestParam(name = "ttl", defaultValue = "600") long ttlSeconds
    ) {
        long now = Instant.now().getEpochSecond();
        long exp = now + Math.max(60, ttlSeconds);

        List<String> roles = Stream.of(rolesCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = toJson(Map.of(
                "preferred_username", user,
                "roles", roles,
                "scope", scope,
                "iat", now,
                "exp", exp
        ));

        String token = signHs256(jwtSecret, headerJson, payloadJson);

        Map<String, Object> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("expires_at", exp);
        resp.put("roles", roles);
        return resp;
    }

    private static String signHs256(String secret, String headerJson, String payloadJson) {
        String h = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String p = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = h + "." + p;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
            String s = base64Url(sig);
            return signingInput + "." + s;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign token", e);
        }
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String toJson(Map<String, ?> map) {
        // Minimal JSON builder to avoid adding dependencies.
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
            } else if (v instanceof List<?> list) {
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
