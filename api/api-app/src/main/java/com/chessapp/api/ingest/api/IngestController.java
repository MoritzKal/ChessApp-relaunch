package com.chessapp.api.ingest.api;

import com.chessapp.api.ingest.api.dto.CreateIngestRequest;
import com.chessapp.api.ingest.api.dto.CreateIngestResponse;
import com.chessapp.api.ingest.api.dto.IngestStatusResponse;
import com.chessapp.api.ingest.entity.IngestRunEntity;
import com.chessapp.api.ingest.service.IngestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping(path = "/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateIngestResponse start(@RequestBody CreateIngestRequest req) {
        UUID runId = ingestService.start(req.username(), req.range());
        return new CreateIngestResponse(runId);
    }

    @GetMapping(path = "/ingest/{runId}")
    public IngestStatusResponse status(@PathVariable UUID runId) {
        IngestRunEntity run = ingestService.get(runId);
        return new IngestStatusResponse(run.getStatus().name(), run.getReportUri());
    }

    @PostMapping(path = "/data/import")
    public ResponseEntity<Void> alias() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .location(URI.create("/v1/ingest"))
                .build();
    }
}
