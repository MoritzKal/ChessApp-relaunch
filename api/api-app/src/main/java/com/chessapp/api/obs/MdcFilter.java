package com.chessapp.api.obs;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

@Component
@Order(40) // früh ausführen
public class MdcFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    try {
      MDC.put("component", "api");
      copy(req, "X-Request-Id", "request_id");
      copy(req, "X-Run-Id", "run_id");
      copy(req, "X-Dataset-Id", "dataset_id");
      copy(req, "X-Model-Id", "model_id");
      Optional.ofNullable(req.getUserPrincipal()).map(Principal::getName).ifPresent(u -> MDC.put("username", u));
      chain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
  private static void copy(HttpServletRequest req, String header, String mdcKey) {
    String v = req.getHeader(header);
    if (v != null && !v.isBlank()) MDC.put(mdcKey, v);
  }
}

