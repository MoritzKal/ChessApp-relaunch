package com.chessapp.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class MonitoringTokenAuthFilter extends OncePerRequestFilter {
  @Value("${security.monitoring.token}")
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
        Authentication a = new AbstractAuthenticationToken(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
          @Override public Object getCredentials() { return token; }
          @Override public Object getPrincipal() { return "monitoring"; }
          @Override public boolean isAuthenticated() { return true; }
        };
        SecurityContextHolder.getContext().setAuthentication(a);
        chain.doFilter(req, res);
        return;
      }
    }
    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }
}

