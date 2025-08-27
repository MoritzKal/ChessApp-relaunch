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
            "X-Model-Id", "model_id",
            "X-Username", "username",
            "X-Component", "component"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest http) {
                HEADER_MAPPINGS.forEach((header, mdcKey) -> {
                    String value = Optional.ofNullable(http.getHeader(header)).orElse(null);
                    if (value != null && !value.isEmpty()) {
                        MDC.put(mdcKey, value);
                    }
                });
            }
            if (MDC.get("username") == null) {
                MDC.put("username", "M3NG00S3");
            }
            if (MDC.get("component") == null) {
                MDC.put("component", "api");
            }
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
