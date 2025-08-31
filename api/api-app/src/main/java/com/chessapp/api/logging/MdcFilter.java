package com.chessapp.api.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.UUID;

@Component
@Order(5)
public class MdcFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws java.io.IOException, jakarta.servlet.ServletException {
    String rid = req.getHeader("X-Request-Id");
    MDC.put("request_id", rid != null ? rid : UUID.randomUUID().toString());
    MDC.put("component", "api");
    MDC.put("path", req.getRequestURI());
    MDC.put("method", req.getMethod());
    MDC.put("username", req.getHeader("X-User"));
    MDC.put("run_id", req.getHeader("X-Run-Id"));
    MDC.put("dataset_id", req.getHeader("X-Dataset-Id"));
    MDC.put("model_id", req.getHeader("X-Model-Id"));
    try { chain.doFilter(req, res); }
    finally {
      MDC.put("status", Integer.toString(res.getStatus()));
      MDC.clear();
    }
  }
}
