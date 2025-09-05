package com.chessapp.api.chesscom.api;

import com.chessapp.api.chesscom.api.dto.ArchiveMetaDto;
import com.chessapp.api.chesscom.api.dto.ArchivesDto;
import com.chessapp.api.chesscom.api.dto.ChessComIngestRequest;
import com.chessapp.api.chesscom.api.dto.ChessComIngestResponse;
import com.chessapp.api.chesscom.service.ChessComService;
import com.chessapp.api.ingest.api.IngestController;
import com.chessapp.api.ingest.api.dto.IngestStartResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/v1")
public class ChessComController {

    private static final Logger log = LoggerFactory.getLogger(ChessComController.class);

    private final ChessComService service;
    private final IngestController ingestController;

    public ChessComController(ChessComService service, IngestController ingestController) {
        this.service = service;
        this.ingestController = ingestController;
    }

    private static String validateUser(String user) {
        if (user == null || !user.matches("[a-z0-9_-]+")) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid user");
        }
        return user.toLowerCase();
    }

    @GetMapping("/chesscom/archives")
    public ArchivesDto archives(@RequestParam("user") String user) {
        String u = validateUser(user);
        try {
            return new ArchivesDto(service.listArchives(u));
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bitte später erneut");
            }
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_GATEWAY);
        }
    }

    @GetMapping("/chesscom/archive/meta")
    public ArchiveMetaDto meta(@RequestParam("user") String user,
                               @RequestParam("year") int year,
                               @RequestParam("month") int month) {
        String u = validateUser(user);
        try {
            ChessComService.ArchiveMeta meta = service.meta(u, year, month);
            return new ArchiveMetaDto(meta.count(), meta.timeControlDist());
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bitte später erneut");
            }
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_GATEWAY);
        }
    }

    @PostMapping("/ingest/chesscom")
    public ResponseEntity<ChessComIngestResponse> ingest(@RequestBody ChessComIngestRequest req) {
        String u = validateUser(req.user());
        List<String> months = req.months();
        if (months == null || months.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "months required");
        }
        String requester = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("chesscom ingest requester={}, user={}, months={}, datasetId={}", requester, u, months, req.datasetId());
        IngestStartResponse last = null;
        for (String m : months) {
            YearMonth.parse(m); // validate format only
            ResponseEntity<IngestStartResponse> resp = ingestController.start(null, req.datasetId(), req.note(), "v" + m);
            last = resp.getBody();
        }
        String runId = last != null ? "ing_" + last.runId().toString() : "ing_unknown";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ChessComIngestResponse(runId, "queued"));
    }
}
