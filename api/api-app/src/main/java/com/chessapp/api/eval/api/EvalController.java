package com.chessapp.api.eval.api;

import java.util.Map;
import java.util.UUID;

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

import com.chessapp.api.eval.api.dto.EvalStartRequest;
import com.chessapp.api.eval.service.EvalService;
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

    public EvalController(EvalService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Start an evaluation",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = EvalStartRequest.class),
                            examples = @ExampleObject(value = "{\n  \"modelId\": \"modelA\",\n  \"datasetId\": \"datasetA\",\n  \"metrics\": [\"accuracy\", \"loss\"]\n}")))
    @ApiResponse(responseCode = "202", description = "Evaluation started",
            content = @Content(schema = @Schema(example = "{\"evalId\":\"123e4567-e89b-12d3-a456-426614174000\"}")))
    public ResponseEntity<?> start(@RequestBody EvalStartRequest body,
                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            UUID evalId = service.start(body, idempotencyKey);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("evalId", evalId.toString()));
        } catch (WebClientResponseException ex) {
            HttpStatus status = ex.getStatusCode().is5xxServerError() ? HttpStatus.SERVICE_UNAVAILABLE : ex.getStatusCode();
            if (ex.getStatusCode().is5xxServerError()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody());
            }
            return ResponseEntity.status(status).body(ex.getResponseBodyAsString());
        } catch (WebClientRequestException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody());
        }
    }

    @GetMapping("/{evalId}")
    @Operation(summary = "Get evaluation status",
            responses = @ApiResponse(responseCode = "200", description = "Evaluation status",
                    content = @Content(examples = @ExampleObject(value = "{\"evalId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"running\"}"))))
    public ResponseEntity<?> get(@PathVariable UUID evalId) {
        try {
            Map<String, Object> resp = service.get(evalId);
            return ResponseEntity.ok(resp);
        } catch (WebClientResponseException ex) {
            HttpStatus status = ex.getStatusCode().is5xxServerError() ? HttpStatus.SERVICE_UNAVAILABLE : ex.getStatusCode();
            if (ex.getStatusCode().is5xxServerError()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody());
            }
            return ResponseEntity.status(status).body(ex.getResponseBodyAsString());
        } catch (WebClientRequestException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody());
        }
    }

    private static Map<String, String> errorBody() {
        return Map.of("reason", "upstream_failure", "correlationId", "");
    }
}
