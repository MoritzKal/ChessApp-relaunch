package com.chessapp.api.serving;

import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.chessapp.api.serving.dto.ModelsLoadRequest;
import com.chessapp.api.serving.dto.PredictRequest;
import com.chessapp.api.serving.dto.PredictResponse;
import com.chessapp.api.storage.MinioStorageService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1")
public class ServingController {

    private static final Logger log = LoggerFactory.getLogger(ServingController.class);

    private final ServingClient client;
    private final MeterRegistry registry;
    private final String defaultUsername;
    private final MinioStorageService storage;

    public ServingController(ServingClient client, MeterRegistry registry,
                             @Value("${chs.default-username:M3NG00S3}") String defaultUsername,
                             MinioStorageService storage) {
        this.client = client;
        this.registry = registry;
        this.defaultUsername = defaultUsername;
        this.storage = storage;
    }

    @PostMapping("/predict")
    public ResponseEntity<?> predict(@Valid @RequestBody PredictRequest body,
                                     @RequestHeader(value = "X-Run-Id", required = false) String runId,
                                     @RequestHeader(value = "X-Username", required = false) String username) {
        String rid = runId != null ? runId : UUID.randomUUID().toString();
        String user = username != null ? username : defaultUsername;
        MDC.put("run_id", rid);
        MDC.put("username", user);
        MDC.put("component", "serve");
        log.info("event=predict.requested");
        Timer.Sample sample = Timer.start(registry);
        try {
            PredictResponse resp = client.predict(body, rid, user);
            String modelId = resp.modelId() != null ? resp.modelId() : "unknown";
            String modelVersion = resp.modelVersion() != null ? resp.modelVersion() : "0";
            sample.stop(Timer.builder("chs_predict_latency_seconds")
                    .tags("username", user, "model_id", modelId, "status", "ok")
                    .register(registry));
            Counter.builder("chs_predict_requests_total")
                    .tags("model_id", modelId, "model_version", modelVersion)
                    .register(registry)
                    .increment();
            log.info("event=predict.completed");
            return ResponseEntity.ok(resp);
        } catch (WebClientResponseException ex) {
            String modelId = "unknown";
            String modelVersion = "0";
            sample.stop(Timer.builder("chs_predict_latency_seconds")
                    .tags("username", user, "model_id", modelId, "status", "error")
                    .register(registry));
            Counter.builder("chs_predict_requests_total")
                    .tags("model_id", modelId, "model_version", modelVersion)
                    .register(registry)
                    .increment();
            log.warn("event=predict.failed");
            HttpStatus status = ex.getStatusCode().is5xxServerError() ? HttpStatus.BAD_GATEWAY : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(ex.getResponseBodyAsString());
        } catch (WebClientRequestException ex) {
            // Connection errors, DNS failures, timeouts before response -> 502 Bad Gateway
            String resbody = String.format("{\"status\":502,\"error\":\"Bad Gateway\",\"message\":%s}",
                    jsonEscape(ex.getMessage()));
            log.warn("event=predict.failed cause=connect_error msg={}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(resbody);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/models/load")
    public ResponseEntity<?> modelsLoad(@RequestBody ModelsLoadRequest body,
                                        @RequestHeader(value = "X-Run-Id", required = false) String runId,
                                        @RequestHeader(value = "X-Username", required = false) String username) {
        String rid = runId != null ? runId : UUID.randomUUID().toString();
        String user = username != null ? username : defaultUsername;
        MDC.put("run_id", rid);
        MDC.put("username", user);
        MDC.put("component", "serve");
        try {
            Map<String, Object> resp = client.modelsLoad(body, rid, user);
            return ResponseEntity.ok(resp);
        } catch (WebClientResponseException ex) {
            HttpStatus status = ex.getStatusCode().is5xxServerError() ? HttpStatus.BAD_GATEWAY : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(ex.getResponseBodyAsString());
        } catch (WebClientRequestException ex) {
            String resbody = String.format("{\"status\":502,\"error\":\"Bad Gateway\",\"message\":%s}",
                    jsonEscape(ex.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(resbody);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/models/promote")
    public ResponseEntity<Map<String, Object>> modelsPromote(@RequestBody Map<String, String> body) {
        String modelId = body.get("modelId");
        String modelVersion = body.getOrDefault("modelVersion", "v1");
        String runId = body.get("runId");
        String artifactUri = body.get("artifactUri");
        if (modelId == null || modelVersion == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "modelId and modelVersion required"));
        }
        try {
            byte[] src;
            if (artifactUri != null && artifactUri.startsWith("s3://")) {
                src = storage.readUri(artifactUri);
            } else if (runId != null) {
                // Default training artifact location
                src = storage.read("mlflow", "models/" + runId + "/best.pt");
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "runId or artifactUri required"));
            }
            if (src == null || src.length == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "source artifact empty or missing"));
            }
            String destKey = "models/" + modelId + "/" + modelVersion + "/best.pt";
            storage.write("mlflow", destKey, src, "application/octet-stream");
            return ResponseEntity.ok(Map.of("ok", true, "artifactUri", "s3://mlflow/" + destKey));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "null";
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
