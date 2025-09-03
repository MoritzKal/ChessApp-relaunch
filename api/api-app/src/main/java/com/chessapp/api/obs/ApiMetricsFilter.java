package com.chessapp.api.obs;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiMetricsFilter implements Filter {
  private final MeterRegistry registry;
  public ApiMetricsFilter(MeterRegistry registry) { this.registry = registry; }

  @Override
  public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    long start = System.nanoTime();
    try {
      chain.doFilter(request, response);
    } finally {
      int status = res.getStatus();
      String method = req.getMethod();
      String uri = (String) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      if (uri == null) uri = req.getRequestURI();
      uri = uri.replaceAll("/\\d+", "/{id}")
               .replaceAll("/[0-9a-fA-F-]{8,}", "/{id}");
      String outcome = status >= 500 ? "error" : (status >= 400 ? "client_error" : "success");
      String userKind = Optional.ofNullable(req.getUserPrincipal()).map(Principal::getName).isPresent() ? "auth" : "anon";
      Tags tags = Tags.of("method", method, "uri", uri, "status", String.valueOf(status), "outcome", outcome, "user_kind", userKind);
      registry.counter("chs_api_requests_total", tags).increment();
      registry.timer("chs_api_request_seconds", tags).record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }
  }
}

