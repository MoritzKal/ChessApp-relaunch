package com.chessapp.api.config;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MDC.put("component", "api");
        String user = request.getHeader("X-Debug-User");
        if (user == null || user.isBlank()) {
            user = "anonymous";
        }
        MDC.put("username", user); // TODO: später JWT aus SecurityContext
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
