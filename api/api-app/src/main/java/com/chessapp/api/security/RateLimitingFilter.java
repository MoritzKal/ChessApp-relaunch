package com.chessapp.api.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter implements Filter {
  private record Counter(AtomicInteger n, long resetAtMs) {}
  private final Cache<String, Counter> cache = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(1)).build();
  // Limits: 30 req/min pro IP auf POST datasets/ingest
  private static final int LIMIT = 30;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    String uri = req.getRequestURI();
    boolean watched = "POST".equals(req.getMethod()) &&
        (uri.equals("/v1/datasets") || uri.equals("/v1/ingest"));
    if (!watched) { chain.doFilter(request, response); return; }
    String key = (req.getRemoteAddr() == null ? "unknown" : req.getRemoteAddr()) + ":" + uri;
    long now = System.currentTimeMillis();
    Counter c = cache.get(key, k -> new Counter(new AtomicInteger(0), now + 60_000));
    if (now > c.resetAtMs()) {
      c.n().set(0);
    }
    if (c.n().incrementAndGet() > LIMIT) {
      HttpServletResponse resp = (HttpServletResponse) response;
      resp.setStatus(429);
      response.getWriter().write("rate limit exceeded");
      return;
    }
    chain.doFilter(request, response);
  }
}

