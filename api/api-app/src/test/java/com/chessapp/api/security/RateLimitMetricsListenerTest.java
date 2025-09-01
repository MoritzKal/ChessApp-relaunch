package com.chessapp.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class RateLimitMetricsListenerTest {

    static class BucketFullException extends RuntimeException {}

    @Test
    void incrementsCounterOnBucketFull() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimitMetricsListener filter = new RateLimitMetricsListener(registry);
        HttpServletRequest req = new MockHttpServletRequest();
        HttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = (r, s) -> { throw new BucketFullException(); };

        assertThrows(BucketFullException.class, () -> filter.doFilter(req, res, chain));
        assertThat(registry.counter("chs_security_rate_limited_total").count()).isEqualTo(1.0);
    }
}
