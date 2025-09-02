package com.chessapp.api.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Configuration
public class MdcConfig implements WebMvcConfigurer {

    private final MeterRegistry registry;
    private final ObjectMapper mapper = new ObjectMapper();

    public MdcConfig(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MdcInterceptor());
    }

    private class MdcInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            MDC.put("route", request.getRequestURI());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                MDC.put("username", auth.getName());
            }
            Map<String, String> pathVars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (pathVars != null) {
                if (pathVars.containsKey("runId")) MDC.put("run_id", pathVars.get("runId"));
                if (pathVars.containsKey("evalId")) MDC.put("eval_id", pathVars.get("evalId"));
                if (pathVars.containsKey("modelId")) MDC.put("model_id", pathVars.get("modelId"));
            }
            if ("POST".equalsIgnoreCase(request.getMethod()) && request instanceof ContentCachingRequestWrapper wrapper) {
                try {
                    byte[] body = wrapper.getContentAsByteArray();
                    if (body.length > 0) {
                        JsonNode node = mapper.readTree(body);
                        if (node.has("runId")) MDC.put("run_id", node.get("runId").asText());
                        if (node.has("evalId")) MDC.put("eval_id", node.get("evalId").asText());
                        if (node.has("modelId")) MDC.put("model_id", node.get("modelId").asText());
                    }
                } catch (IOException ignore) {
                }
            }
            request.setAttribute("_startTime", System.nanoTime());
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
                throws Exception {
            Long start = (Long) request.getAttribute("_startTime");
            if (start != null) {
                long duration = System.nanoTime() - start;
                Timer.builder("chs_api_request_duration_seconds")
                        .tags("route", request.getRequestURI())
                        .register(registry)
                        .record(duration, TimeUnit.NANOSECONDS);
                io.micrometer.core.instrument.Counter.builder("chs_api_requests_total")
                        .tags("route", request.getRequestURI(),
                                "code", String.valueOf(response.getStatus()),
                                "method", request.getMethod())
                        .register(registry)
                        .increment();
            }
        }
    }
}
