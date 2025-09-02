package com.chessapp.api.selfplay.api;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.chessapp.api.selfplay.api.dto.SelfPlayRunRequest;
import com.chessapp.api.selfplay.service.SelfPlayService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/selfplay")
@Tag(name = "Self-Play")
public class SelfPlayController {

    private final SelfPlayService service;
    private final MeterRegistry registry;

    public SelfPlayController(SelfPlayService service, MeterRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    @PostMapping("/runs")
    @Operation(summary = "Start a self-play run",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = SelfPlayRunRequest.class),
                            examples = @ExampleObject(value = "{\n  \"modelId\": \"modelA\",\n  \"baselineId\": \"baselineA\",\n  \"games\": 100,\n  \"concurrency\": 4,\n  \"seed\": 42\n}")))
    @ApiResponse(responseCode = "201", description = "Run created",
            content = @Content(schema = @Schema(example = "{\"runId\":\"123e4567-e89b-12d3-a456-426614174000\"}")))
    public ResponseEntity<?> start(@RequestBody SelfPlayRunRequest body,
                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                   Principal principal) {
        String route = "/v1/selfplay/runs";
        MDC.put("route", route);
        MDC.put("component", "api");
        MDC.put("username", principal.getName());
        Timer.Sample sample = Timer.start(registry);
        try {
            UUID runId = service.start(body, idempotencyKey);
            MDC.put("run_id", runId.toString());
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", "201").register(registry).increment();
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("runId", runId.toString()));
        } catch (WebClientResponseException ex) {
            HttpStatus status = ex.getStatusCode().is5xxServerError() ? HttpStatus.BAD_GATEWAY : ex.getStatusCode();
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", String.valueOf(status.value())).register(registry).increment();
            return ResponseEntity.status(status).body(ex.getResponseBodyAsString());
        } catch (WebClientRequestException ex) {
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", "502").register(registry).increment();
            String res = String.format("{\"status\":502,\"error\":\"Bad Gateway\",\"message\":%s}", jsonEscape(ex.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(res);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/runs/{runId}")
    @Operation(summary = "Get self-play run status",
            responses = @ApiResponse(responseCode = "200", description = "Run status",
                    content = @Content(examples = @ExampleObject(value = "{\"runId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"running\"}"))))
    public ResponseEntity<?> get(@PathVariable UUID runId, Principal principal) {
        String route = "/v1/selfplay/runs/{runId}";
        MDC.put("route", route);
        MDC.put("component", "api");
        MDC.put("username", principal.getName());
        MDC.put("run_id", runId.toString());
        Timer.Sample sample = Timer.start(registry);
        try {
            Map<String, Object> resp = service.get(runId);
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", "200").register(registry).increment();
            return ResponseEntity.ok(resp);
        } catch (WebClientResponseException ex) {
            HttpStatus status = ex.getStatusCode().is5xxServerError() ? HttpStatus.BAD_GATEWAY : ex.getStatusCode();
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", String.valueOf(status.value())).register(registry).increment();
            return ResponseEntity.status(status).body(ex.getResponseBodyAsString());
        } catch (WebClientRequestException ex) {
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", "502").register(registry).increment();
            String res = String.format("{\"status\":502,\"error\":\"Bad Gateway\",\"message\":%s}", jsonEscape(ex.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(res);
        } finally {
            MDC.clear();
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "null";
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
