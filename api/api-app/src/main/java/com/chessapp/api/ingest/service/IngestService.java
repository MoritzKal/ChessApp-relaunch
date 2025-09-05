package com.chessapp.api.ingest.service;

import com.chessapp.api.data.ingest.IngestRunEntity;
import com.chessapp.api.data.ingest.IngestRunRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service orchestrating ingest runs.
 */
@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final IngestRunRepository repository;
    private final MeterRegistry meterRegistry;
    private final Counter starts;
    private final Counter success;
    private final Counter failed;
    private final Timer durationSuccess;
    private final Timer durationFailed;
    private final AtomicInteger activeGauge;
    private final IngestService self;
    private final com.chessapp.api.chesscom.service.ChessComIngestService chessComIngestService;

    public IngestService(IngestRunRepository repository, MeterRegistry meterRegistry,
                         @Lazy IngestService self,
                         com.chessapp.api.chesscom.service.ChessComIngestService chessComIngestService) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
        this.starts = meterRegistry.counter("chs_ingest_starts_total");
        this.success = meterRegistry.counter("chs_ingest_success_total");
        this.failed = meterRegistry.counter("chs_ingest_failed_total");
        this.durationSuccess = Timer.builder("chs_ingest_duration_seconds")
            .publishPercentileHistogram()
            .publishPercentiles(0.5, 0.95)
            .tags("outcome","success")
            .register(meterRegistry);
         this.durationFailed = Timer.builder("chs_ingest_duration_seconds")
            .publishPercentileHistogram()
            .publishPercentiles(0.5, 0.95)
            .tags("outcome","failed")
            .register(meterRegistry);
        this.activeGauge = meterRegistry.gauge("chs_ingest_active", new AtomicInteger());
        this.self = self;
        this.chessComIngestService = chessComIngestService;
    }

    /** Resolve current username from SecurityContext, fallback to CHESS_USERNAME env or "system". */
    private static String currentUsername() {
        var ctx = SecurityContextHolder.getContext();
        var auth = (ctx != null) ? ctx.getAuthentication() : null;
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return auth.getName();
        }
        return Optional.ofNullable(System.getenv("CHESS_USERNAME")).orElse("system");
    }

    /**
     * Parse month values from ENV. Supports either:
     * - plain month 1..12 (e.g. "9"), or
     * - yyyymm compact form (e.g. "202509").
     * We simply store the parsed integer; the entity/DB column type must match.
     */
    private static int parseMonthEnvOrDefault(String key, int fallback) {
        var v = System.getenv(key);
        if (v == null || v.isBlank()) return fallback;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Default strategy: both months = current UTC month (either 9 or 202509 depending on schema expectation). */
    private static int defaultMonthValue() {
        // If your DB column stores only 1..12, this returns 1..12.
        // If your DB stores yyyymm, prefer using INGEST_FROM_MONTH/INGEST_TO_MONTH ENV to pass yyyymm explicitly.
        return YearMonth.now(ZoneOffset.UTC).getMonthValue();
    }

    /**
     * Starts a new ingest run and schedules async execution.
     *
     * @return run identifier
     */
    public UUID start(String datasetId, String version) {
        UUID runId = UUID.randomUUID();

        // Resolve required NOT NULLs
        int defaultMonth = defaultMonthValue();
        int fromMonth = parseMonthEnvOrDefault("INGEST_FROM_MONTH", defaultMonth);
        int toMonth   = parseMonthEnvOrDefault("INGEST_TO_MONTH",   fromMonth);

        IngestRunEntity run = new IngestRunEntity();
        run.setRunId(runId);
        run.setUsername(currentUsername());     // NOT NULL
        run.setFromMonth(fromMonth);            // NOT NULL in DB
        run.setToMonth(toMonth);                // likely NOT NULL as well (safe to set)
        run.setStatus("PENDING");
        run.setStartedAt(Instant.now());
        run.setDatasetId(datasetId);
        run.setVersion(version);
        if (version != null) {
            run.setVersions(java.util.List.of(version));
        } else {
            run.setVersions(java.util.List.of());
        }
        run.setFilesWritten(0L);
        repository.save(run);

        starts.increment();

        MDC.put("run_id", runId.toString());
        if (datasetId != null) MDC.put("dataset_id", datasetId);
        MDC.put("username", run.getUsername());
        MDC.put("component", "ingest-start");
        try {
            self.execute(runId);                // trigger async
        } finally {
            MDC.clear();
        }
        return runId;
    }

    /**
     * Asynchronous execution of an ingest run.
     */
    @Async("ingestExecutor")
    public void execute(UUID runId) {
        activeGauge.incrementAndGet();
        Timer.Sample sample = Timer.start(meterRegistry);
        IngestRunEntity run = repository.findById(runId).orElseThrow();
        MDC.put("run_id", runId.toString());
        if (run.getDatasetId() != null) MDC.put("dataset_id", run.getDatasetId());
        if (run.getUsername() != null) MDC.put("username", run.getUsername());
        MDC.put("component", "ingest-execute");
        try {
            update(runId, "RUNNING", null);
            log.info("ingest run {} running", runId);

            if (run.getDatasetId() != null && run.getUsername() != null
                    && run.getVersions() != null && !run.getVersions().isEmpty()) {
                List<YearMonth> months = run.getVersions().stream()
                        .map(v -> YearMonth.parse(v.substring(1)))
                        .toList();
                chessComIngestService.ingest(runId, run.getDatasetId(), run.getUsername(), months);
            } else {
                // simulate work when no dataset context
                Thread.sleep(1000);
            }

            String report = "s3://reports/ingest/" + runId + "/report.json";
            update(runId, "SUCCEEDED", report);
            success.increment();
            sample.stop(durationSuccess);
            log.info("ingest run {} succeeded", runId);
        } catch (Exception e) {
            update(runId, "FAILED", null);
            failed.increment();
            sample.stop(durationFailed);
            log.error("ingest run {} failed: {}", runId, e.getMessage());
        } finally {
            MDC.clear();
            activeGauge.decrementAndGet();
        }
    }

    private void update(UUID runId, String status, String reportUri) {
        IngestRunEntity run = repository.findById(runId).orElseThrow();
        run.setStatus(status);
        if (reportUri != null) {
            run.setReportUri(reportUri);
        }
        if ("SUCCEEDED".equals(status) || "FAILED".equals(status)) {
            run.setFinishedAt(Instant.now());
        }
        repository.save(run);
    }
}
