package com.chessapp.api.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class MdcFilter extends OncePerRequestFilter {
    private static final String K_RUN = "run_id";
    private static final String K_DATASET = "dataset_id";
    private static final String K_MODEL = "model_id";
    private static final String K_USER = "username";
    private static final String K_COMPONENT = "component";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Map<String, String> vals = Map.of(
                K_RUN, header(req, "X-Run-Id"),
                K_DATASET, header(req, "X-Dataset-Id"),
                K_MODEL, header(req, "X-Model-Id"),
                K_USER, header(req, "X-User"),
                K_COMPONENT, "api"
        );
        try {
            vals.forEach((k, v) -> { if (v != null && !v.isBlank()) MDC.put(k, v); });
            chain.doFilter(req, res);
        } finally {
            vals.keySet().forEach(MDC::remove);
        }
    }

    private static String header(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return (v == null || v.isBlank()) ? null : v;
    }
}

