package com.chessapp.api.ingest;

import com.chessapp.api.ingest.dto.CreateIngestRequest;
import com.chessapp.api.ingest.dto.CreateIngestResponse;
import com.chessapp.api.ingest.dto.IngestStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/ingest")
@SecurityRequirement(name = "bearerAuth")
public class IngestController {

    private final IngestService service;

    public IngestController(IngestService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Start offline ingest", description = "Startet einen Ingest-Run (offline Slice).")
    @ApiResponses({@ApiResponse(responseCode = "202", description = "accepted")})
    public CreateIngestResponse start(@Valid @RequestBody CreateIngestRequest request) {
        UUID runId = service.startRun(request.username(), request.range());
        return new CreateIngestResponse(runId);
    }

    @GetMapping(value = "/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Poll ingest run status")
    public IngestStatusResponse status(@PathVariable UUID runId) {
        return service.getStatus(runId);
    }
}
