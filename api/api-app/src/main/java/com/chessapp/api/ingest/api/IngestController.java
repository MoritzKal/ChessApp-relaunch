package com.chessapp.api.ingest.api;

import com.chessapp.api.ingest.api.dto.IngestStartResponse;
import com.chessapp.api.ingest.api.dto.IngestStatusResponse;
import com.chessapp.api.ingest.service.IngestService;
import com.chessapp.api.data.ingest.IngestRunEntity;
import com.chessapp.api.data.ingest.IngestRunRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for ingest runs.
 */
@RestController
@RequestMapping("/v1/ingest")
@Tag(name = "Ingest", description = "Start and monitor ingest runs")
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final IngestService ingestService;
    private final IngestRunRepository repository;

    public IngestController(IngestService ingestService, IngestRunRepository repository) {
        this.ingestService = ingestService;
        this.repository = repository;
    }

    @PostMapping
    @Operation(
        summary = "Start ingest run",
        responses = @ApiResponse(
            responseCode = "202",
            description = "Run accepted",
            headers = @Header(name = "Location", description = "Poll URI: /v1/ingest/{runId}"),
            content = @Content(schema = @Schema(implementation = IngestStartResponse.class))
        )
    )
    public ResponseEntity<IngestStartResponse> start() {
        UUID runId = ingestService.start();
        log.info("ingest run {} started", runId);
        return ResponseEntity.accepted()
            .location(URI.create("/v1/ingest/" + runId))
            .body(new IngestStartResponse(runId));
    }

    @GetMapping("/{runId}")
    @Operation(
        summary = "Get ingest run status",
        responses = @ApiResponse(
            responseCode = "200",
            description = "Current run status",
            content = @Content(schema = @Schema(implementation = IngestStatusResponse.class))
        )
    )
    public ResponseEntity<IngestStatusResponse> status(@PathVariable UUID runId) {
        Optional<IngestRunEntity> opt = repository.findById(runId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        IngestRunEntity run = opt.get();
        return ResponseEntity.ok(new IngestStatusResponse(run.getStatus(), run.getReportUri()));
    }
}
