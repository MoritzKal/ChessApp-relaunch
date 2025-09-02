package com.chessapp.api.models.api;

import com.chessapp.api.models.service.ModelNotFoundException;
import com.chessapp.api.models.service.RegistryUnavailableException;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = ModelController.class)
public class RegistryExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RegistryExceptionHandler.class);

    private final MeterRegistry metrics;

    public RegistryExceptionHandler(MeterRegistry metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ModelNotFoundException ex) {
        metrics.counter("chs_model_registry_requests_total", "endpoint", Objects.toString(MDC.get("endpoint"), "unknown"), "status", "404")
                .increment();
        log.warn("registry.error", ex);
        MDC.clear();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "model_not_found", "message", ex.getMessage()));
    }

    @ExceptionHandler(RegistryUnavailableException.class)
    public ResponseEntity<Map<String, Object>> regDown(RegistryUnavailableException ex) {
        metrics.counter("chs_model_registry_requests_total", "endpoint", Objects.toString(MDC.get("endpoint"), "unknown"), "status", "500")
                .increment();
        log.error("registry.error", ex);
        MDC.clear();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "registry_unavailable", "message", ex.getMessage()));
    }
}
