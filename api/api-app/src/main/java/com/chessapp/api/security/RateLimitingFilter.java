package com.chessapp.api.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.*;
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
  private static final int LIMIT = 30; // req/min

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    String uri = req.getRequestURI();
    boolean watched = "POST".equals(req.getMethod()) &&
        ("/v1/datasets".equals(uri) || "/v1/ingest".equals(uri));
    if (!watched) { chain.doFilter(request, response); return; }
    String ip = req.getRemoteAddr() == null ? "unknown" : req.getRemoteAddr();
    String key = ip + ":" + uri;
    long now = System.currentTimeMillis();
    Counter c = cache.get(key, k -> new Counter(new AtomicInteger(0), now + 60_000));
    if (now > c.resetAtMs()) c.n().set(0);
    if (c.n().incrementAndGet() > LIMIT) {
      ((HttpServletResponse) response).setStatus(429);
      response.getWriter().write("rate limit exceeded");
      return;
    }
    chain.doFilter(request, response);
  }
}
