package com.chessapp.api.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class IdempotencyFilter extends OncePerRequestFilter {

    private record CachedResponse(int status, byte[] body, Map<String, List<String>> headers) {}

    private final Cache<String, CachedResponse> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .maximumSize(10_000)
            .build();

    private final MeterRegistry registry;

    public IdempotencyFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !request.getRequestURI().startsWith("/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper respWrapper = new ContentCachingResponseWrapper(response);
        String idemKey = request.getHeader("Idempotency-Key");
        if (idemKey == null || idemKey.isBlank()) {
            filterChain.doFilter(reqWrapper, respWrapper);
            response.addHeader("Idempotent-Replay", "false");
            respWrapper.copyBodyToResponse();
            return;
        }

        byte[] bodyBytes = StreamUtils.copyToByteArray(reqWrapper.getInputStream());
        String bodyHash = sha256(bodyBytes);
        String principal = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            principal = auth.getName();
        }
        String cacheKey = principal + ":" + request.getMethod() + ":" + request.getRequestURI() + ":" + bodyHash + ":" + idemKey;
        CachedResponse cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            response.setStatus(cached.status());
            cached.headers().forEach((h, v) -> {
                if (!"Idempotent-Replay".equalsIgnoreCase(h)) {
                    v.forEach(val -> response.addHeader(h, val));
                }
            });
            response.setHeader("Idempotent-Replay", "true");
            registry.counter("chs_api_requests_total", "route", request.getRequestURI(), "code", String.valueOf(cached.status()), "method", request.getMethod()).increment();
            Timer.builder("chs_api_request_duration_seconds").tags("route", request.getRequestURI()).register(registry).record(0, TimeUnit.MILLISECONDS);
            response.getOutputStream().write(cached.body());
            return;
        }

        long start = System.nanoTime();
        filterChain.doFilter(reqWrapper, respWrapper);
        long durationNs = System.nanoTime() - start;

        response.addHeader("Idempotent-Replay", "false");
        byte[] respBytes = respWrapper.getContentAsByteArray();
        Map<String, List<String>> headers = new HashMap<>();
        for (String h : respWrapper.getHeaderNames()) {
            if (!"Idempotent-Replay".equalsIgnoreCase(h)) {
                headers.put(h, new ArrayList<>(respWrapper.getHeaders(h)));
            }
        }
        cache.put(cacheKey, new CachedResponse(respWrapper.getStatus(), respBytes, headers));
        registry.counter("chs_api_requests_total", "route", request.getRequestURI(), "code", String.valueOf(respWrapper.getStatus()), "method", request.getMethod()).increment();
        Timer.builder("chs_api_request_duration_seconds").tags("route", request.getRequestURI()).register(registry).record(durationNs, TimeUnit.NANOSECONDS);
        respWrapper.copyBodyToResponse();
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not supported", e);
        }
    }
}
