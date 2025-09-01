package com.chessapp.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class MonitoringTokenAuthFilter extends OncePerRequestFilter {
  @Value("${security.monitoring.token:monitoring-test-token}")
  private String monitoringToken;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"/actuator/prometheus".equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String auth = req.getHeader("Authorization");
    if (auth != null && auth.startsWith("Bearer ")) {
      String token = auth.substring("Bearer ".length());
      if (token.equals(monitoringToken)) {
        chain.doFilter(req, res);
        return;
      }
    }
    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }
}
