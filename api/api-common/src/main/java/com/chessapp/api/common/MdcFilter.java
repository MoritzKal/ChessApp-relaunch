package com.chessapp.api.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Populate basic MDC labels from JWT or debug headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter implements Filter {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest http) {
                MDC.put("username", extractUsername(http));
                MDC.put("component", "api");
            }
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String extractUsername(HttpServletRequest http) {
        String auth = http.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                    Map<?, ?> claims = mapper.readValue(payload, Map.class);
                    Object preferred = claims.get("preferred_username");
                    Object sub = claims.get("sub");
                    if (preferred != null) {
                        return preferred.toString();
                    }
                    if (sub != null) {
                        return sub.toString();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        String debug = http.getHeader("X-Debug-User");
        if (debug != null && !debug.isBlank()) {
            return debug;
        }
        return "anonymous";
    }
}
