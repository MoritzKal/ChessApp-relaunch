package com.chessapp.api.ingest.api;

import com.chessapp.api.ingest.api.dto.CreateIngestRequest;
import com.chessapp.api.ingest.api.dto.CreateIngestResponse;
import com.chessapp.api.ingest.api.dto.IngestStatusResponse;
import com.chessapp.api.ingest.service.IngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/ingest")
@Tag(name = "Ingest")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Start offline ingest", description = "Startet einen Ingest-Run (offline Slice).")
    @ApiResponses(@ApiResponse(responseCode = "202", description = "Accepted"))
    public CreateIngestResponse start(@Valid @RequestBody CreateIngestRequest req) {
        UUID runId = ingestService.startRun(req.username(), req.range());
        return new CreateIngestResponse(runId);
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Poll ingest run status")
    public IngestStatusResponse status(@PathVariable UUID runId) {
        return ingestService.getStatus(runId);
    }
}
