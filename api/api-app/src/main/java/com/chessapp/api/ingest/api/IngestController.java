package com.chessapp.api.ingest.api;

import com.chessapp.api.ingest.api.dto.IngestStartResponse;
import com.chessapp.api.ingest.api.dto.IngestStatusResponse;
import com.chessapp.api.ingest.service.IngestService;
import com.chessapp.api.data.ingest.IngestRunEntity;
import com.chessapp.api.data.ingest.IngestRunRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for ingest runs.
 */
@RestController
@RequestMapping({"/v1/ingest", "/v1/data/import"})
@Tag(name = "Ingest", description = "Start and monitor ingest runs")
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final IngestService ingestService;
    private final IngestRunRepository repository;

    public IngestController(IngestService ingestService, IngestRunRepository repository) {
        this.ingestService = ingestService;
        this.repository = repository;
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Start ingest run")
    public ResponseEntity<IngestStartResponse> start(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "datasetId", required = false) String datasetId,
            @RequestPart(value = "note", required = false) String note,
            @RequestPart(value = "version", required = false) String version
    ) {
        UUID runId = ingestService.start(datasetId, version);
        log.info("ingest run {} started", runId);
        URI location = URI.create("/v1/ingest/" + runId);
        return ResponseEntity.created(location)
                .body(new IngestStartResponse(runId, "queued"));
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Get ingest run status", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = IngestStatusResponse.class))))
    public ResponseEntity<IngestStatusResponse> status(@PathVariable String runId) {
        UUID id;
        try {
            String cleaned = runId != null && runId.startsWith("ing_") ? runId.substring(4) : runId;
            id = UUID.fromString(cleaned);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
        Optional<IngestRunEntity> opt = repository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        IngestRunEntity run = opt.get();
        return ResponseEntity.ok(
                new IngestStatusResponse(
                        runId,
                        run.getStatus(),
                        run.getDatasetId(),
                        run.getVersions() != null ? run.getVersions() : java.util.List.of(),
                        run.getReportUri(),
                        run.getFilesWritten()
                )
        );
    }
}
