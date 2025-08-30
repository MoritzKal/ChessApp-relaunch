package com.chessapp.api.models.api;

import com.chessapp.api.models.api.dto.ModelSummary;
import com.chessapp.api.models.api.dto.ModelVersionSummary;
import com.chessapp.api.models.service.ModelRegistryService;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@Tag(name = "Model Registry (read-only)")
public class ModelsController {

    private static final Logger log = LoggerFactory.getLogger(ModelsController.class);

    private final ModelRegistryService service;
    private final MeterRegistry metrics;

    public ModelsController(ModelRegistryService service, MeterRegistry metrics) {
        this.service = service;
        this.metrics = metrics;
    }

    @Operation(summary = "List available models")
    @GetMapping("/models")
    public ResponseEntity<List<ModelSummary>> listModels(
            @RequestHeader(value = "X-Username", required = false) String username) {
        MDC.put("run_id", UUID.randomUUID().toString());
        MDC.put("component", "api.registry");
        MDC.put("username", username != null ? username : MDC.get("username"));
        MDC.put("endpoint", "/v1/models");
        var out = service.listModels();
        log.info("registry.request");
        metrics.counter("chs_model_registry_requests_total", "endpoint", "/v1/models", "status", "200")
                .increment();
        MDC.clear();
        return ResponseEntity.ok(out);
    }

    @Operation(summary = "List versions for a given model id")
    @GetMapping("/models/{id}/versions")
    public ResponseEntity<List<ModelVersionSummary>> listVersions(
            @PathVariable("id") String modelId,
            @RequestHeader(value = "X-Username", required = false) String username) {
        MDC.put("run_id", UUID.randomUUID().toString());
        MDC.put("component", "api.registry");
        MDC.put("username", username != null ? username : MDC.get("username"));
        MDC.put("endpoint", "/v1/models/{id}/versions");
        MDC.put("model_id", modelId);
        var out = service.listVersions(modelId);
        log.info("registry.request");
        metrics.counter("chs_model_registry_requests_total", "endpoint", "/v1/models/{id}/versions", "status", "200")
                .increment();
        MDC.clear();
        return ResponseEntity.ok(out);
    }
}
