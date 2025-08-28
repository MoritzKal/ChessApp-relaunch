package com.chessapp.api.ingest.api;

import com.chessapp.api.ingest.api.dto.IngestRequest;
import com.chessapp.api.ingest.entity.IngestRun;
import com.chessapp.api.ingest.repo.IngestRunRepository;
import com.chessapp.api.ingest.service.IngestService;
import com.chessapp.api.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/ingest")
@Tag(name = "ingest")
public class IngestController {

    private final IngestService ingestService;
    private final IngestRunRepository ingestRunRepository;

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    public IngestController(IngestService ingestService, IngestRunRepository ingestRunRepository) {
        this.ingestService = ingestService;
        this.ingestRunRepository = ingestRunRepository;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Start ingest job", responses = {
            @ApiResponse(responseCode = "202", description = "Job accepted")
    })
    public Map<String, String> start(@RequestBody(required = false) IngestRequest request) {
        log.info("ingestService bean impl={}", ingestService.getClass());
        String username = (request != null && request.username() != null && !request.username().isBlank())
                ? request.username()
                : "M3NG00S3";
        boolean offline = request != null && Boolean.TRUE.equals(request.offline());
        YearMonth from = (request != null && request.from() != null) ? YearMonth.parse(request.from()) : YearMonth.now();
        YearMonth to   = (request != null && request.to()   != null) ? YearMonth.parse(request.to())   : from;

        // falls jemand verdreht liefert
        if (to.isBefore(from)) {
            YearMonth tmp = from;
            from = to;
            to = tmp;
        }

        UUID runId = UUID.randomUUID();
        IngestRun run = new IngestRun();
        run.setId(runId);
        run.setUsername(username);
        run.setFromMonth(from.toString());
        run.setToMonth(to.toString());
        run.setStatus("queued");
        run.setStartedAt(Instant.now());
        // WICHTIG: sofort persistieren, damit der Async-Thread sie sicher findet
        ingestRunRepository.saveAndFlush(run);
        log.info("event=ingest.enqueued run_id={} username={} offline={} from={} to={}",
                runId, username, offline, from, to);
        // Async-Job starten
        ingestService.enqueueIngest(runId, username, from, to, offline);
        return Map.of("runId", runId.toString());
    }

    @GetMapping(value = "/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get ingest status", responses = {
            @ApiResponse(responseCode = "200", description = "Status", content = @Content(schema = @Schema(implementation = Map.class)))
    })
    public Map<String, Object> status(@PathVariable UUID runId) {
        IngestRun run = ingestRunRepository.findById(runId).orElseThrow();
        Map<String, Object> counts = new HashMap<>();
        counts.put("games", run.getGamesCount());
        counts.put("moves", run.getMovesCount());
        counts.put("positions", run.getPositionsCount());
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", run.getStatus());
        resp.put("counts", counts);
        resp.put("startedAt", run.getStartedAt());
        if (run.getFinishedAt() != null) resp.put("finishedAt", run.getFinishedAt());
        if (run.getError() != null)      resp.put("error", run.getError());
        if (run.getReportUri() != null)  resp.put("report_uri", run.getReportUri());
        return resp;
    }
}