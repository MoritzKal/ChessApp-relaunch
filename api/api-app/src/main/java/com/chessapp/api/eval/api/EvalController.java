package com.chessapp.api.eval.api;

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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.chessapp.api.eval.api.dto.EvalStartRequest;
import com.chessapp.api.eval.service.EvalService;

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
@RequestMapping("/v1/evaluations")
@Tag(name = "Evaluations")
public class EvalController {

    private final EvalService service;
    private final MeterRegistry registry;

    public EvalController(EvalService service, MeterRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    @PostMapping
    @Operation(summary = "Start an evaluation",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = EvalStartRequest.class),
                            examples = @ExampleObject(value = "{\n  \"modelId\": \"modelA\",\n  \"datasetId\": \"datasetA\",\n  \"metrics\": [\"accuracy\", \"loss\"]\n}")))
    @ApiResponse(responseCode = "202", description = "Evaluation started",
            content = @Content(schema = @Schema(example = "{\"evalId\":\"123e4567-e89b-12d3-a456-426614174000\"}")))
    public ResponseEntity<?> start(@RequestBody EvalStartRequest body,
                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                   Principal principal) {
        String route = "/v1/evaluations";
        MDC.put("route", route);
        MDC.put("component", "api");
        MDC.put("username", principal.getName());
        Timer.Sample sample = Timer.start(registry);
        try {
            UUID evalId = service.start(body, idempotencyKey);
            MDC.put("eval_id", evalId.toString());
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", "202").register(registry).increment();
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("evalId", evalId.toString()));
        } catch (WebClientResponseException ex) {
            HttpStatus status = ex.getStatusCode().is5xxServerError() ? HttpStatus.SERVICE_UNAVAILABLE : ex.getStatusCode();
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", String.valueOf(status.value())).register(registry).increment();
            if (ex.getStatusCode().is5xxServerError()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody());
            }
            return ResponseEntity.status(status).body(ex.getResponseBodyAsString());
        } catch (WebClientRequestException ex) {
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", "503").register(registry).increment();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody());
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{evalId}")
    @Operation(summary = "Get evaluation status",
            responses = @ApiResponse(responseCode = "200", description = "Evaluation status",
                    content = @Content(examples = @ExampleObject(value = "{\"evalId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"running\"}"))))
    public ResponseEntity<?> get(@PathVariable UUID evalId, Principal principal) {
        String route = "/v1/evaluations/{evalId}";
        MDC.put("route", route);
        MDC.put("component", "api");
        MDC.put("username", principal.getName());
        MDC.put("eval_id", evalId.toString());
        Timer.Sample sample = Timer.start(registry);
        try {
            Map<String, Object> resp = service.get(evalId);
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", "200").register(registry).increment();
            return ResponseEntity.ok(resp);
        } catch (WebClientResponseException ex) {
            HttpStatus status = ex.getStatusCode().is5xxServerError() ? HttpStatus.SERVICE_UNAVAILABLE : ex.getStatusCode();
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", String.valueOf(status.value())).register(registry).increment();
            if (ex.getStatusCode().is5xxServerError()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody());
            }
            return ResponseEntity.status(status).body(ex.getResponseBodyAsString());
        } catch (WebClientRequestException ex) {
            sample.stop(Timer.builder("chs_api_request_duration_seconds").tags("route", route).register(registry));
            Counter.builder("chs_api_requests_total").tags("route", route, "code", "503").register(registry).increment();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody());
        } finally {
            MDC.clear();
        }
    }

    private static Map<String, String> errorBody() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String corr = attrs != null ? attrs.getRequest().getHeader("X-Request-Id") : null;
        return Map.of(
                "reason", "upstream_failure",
                "correlationId", corr == null ? "" : corr
        );
    }
}
