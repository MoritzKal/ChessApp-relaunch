package com.chessapp.api.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/data/import")
public class DataImportController {
    private static final Logger log = LoggerFactory.getLogger(DataImportController.class);
    private final Counter ingestJobs;

    public DataImportController(MeterRegistry registry) {
        // commonTags (application=api, component=api, username=...) kommen aus ObservabilityConfig
        this.ingestJobs = Counter.builder("chs_ingest_jobs_total")
                .description("Number of ingest jobs triggered")
                .register(registry);
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(
            @RequestHeader(value = "X-Run-Id", required = false) String runId,
            @RequestHeader(value = "X-Component", required = false) String component,
            @RequestHeader(value = "X-Username", required = false) String username
    ) {
        ingestJobs.increment();
        // MDC-Felder (run_id, component, username) werden – falls dein MdcFilter aktiv ist – automatisch geloggt.
        log.info("ingest.start accepted");
        return ResponseEntity.accepted().body(Map.of("status", "accepted"));
    }
}
