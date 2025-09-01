package com.chessapp.api.common;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Simple servlet filter to populate MDC labels from HTTP headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter implements Filter {

    private static final Map<String, String> HEADER_MAPPINGS = Map.of(
            "X-Run-Id", "run_id",
            "X-Dataset-Id", "dataset_id",
            "X-Model-Id", "model_id"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            MDC.put("component", "api");
            String username = "anonymous";
            if (request instanceof HttpServletRequest http) {
                HEADER_MAPPINGS.forEach((header, mdcKey) -> {
                    String value = Optional.ofNullable(http.getHeader(header)).orElse(null);
                    if (value != null && !value.isEmpty()) {
                        MDC.put(mdcKey, value);
                    }
                });
                String debugUser = http.getHeader("X-Debug-User");
                if (debugUser != null && !debugUser.isBlank()) {
                    username = debugUser;
                }
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                username = jwtAuth.getName();
            }
            MDC.put("username", username);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
