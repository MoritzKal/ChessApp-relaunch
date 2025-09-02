package com.chessapp.api.codex;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
@Profile("codex")
public class HttpMetricsFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpMetricsFilter.class);

    private final MeterRegistry registry;
    private final String username;

    public HttpMetricsFilter(MeterRegistry registry,
                             @Value("${chs.default-username:M3NG00S3}") String username) {
        this.registry = registry;
        this.username = username;
    }

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String runId = UUID.randomUUID().toString();
        MDC.put("run_id", runId);
        MDC.put("component", "api");
        MDC.put("username", username);
        MDC.put("path", request.getRequestURI());

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            String status = String.valueOf(response.getStatus());

            MDC.put("status", status);
            MDC.put("duration_ms", String.valueOf(durationMs));

            Counter.builder("chs_api_http_requests_total")
                .tags("method", request.getMethod(),
                      "path", request.getRequestURI(),
                      "status", status)
                .register(registry)
                .increment();

            Timer.builder("chs_api_http_latency_ms")
                .tags("method", request.getMethod(),
                      "path", request.getRequestURI(),
                      "status", status)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);

            log.info("request completed");
            MDC.clear();
        }
    }
}

