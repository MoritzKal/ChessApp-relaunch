package com.chessapp.api.ingest.service;

import com.chessapp.api.ingest.IngestRunStatus;
import com.chessapp.api.ingest.api.dto.IngestStatusResponse;
import com.chessapp.api.ingest.entity.IngestRunEntity;
import com.chessapp.api.ingest.repo.IngestRunRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final IngestRunRepository repository;
    private final Counter startedCounter;
    private final Counter succeededCounter;
    private final Counter failedCounter;
    private final AtomicInteger activeRuns;
    private final LongSupplier delaySupplier;
    private final ApplicationContext context;

    public IngestService(IngestRunRepository repository,
                         MeterRegistry registry,
                         LongSupplier ingestDelaySupplier,
                         ApplicationContext context) {
        this.repository = repository;
        this.startedCounter = Counter.builder("chs_ingest_runs_started_total").register(registry);
        this.succeededCounter = Counter.builder("chs_ingest_runs_succeeded_total").register(registry);
        this.failedCounter = Counter.builder("chs_ingest_runs_failed_total").register(registry);
        this.activeRuns = registry.gauge("chs_ingest_active_runs", new AtomicInteger());
        this.delaySupplier = ingestDelaySupplier;
        this.context = context;
    }

    @Transactional
    public UUID startRun(String username, @Nullable String range) {
        UUID runId = UUID.randomUUID();
        try (MDC.MDCCloseable m = MDC.putCloseable("run_id", runId.toString())) {
            log.info("ingest requested for {}", username);
            IngestRunEntity e = new IngestRunEntity();
            e.setId(runId);
            e.setUsername(username);
            e.setRange(range);
            e.setStatus(IngestRunStatus.RUNNING);
            e.setStartedAt(Instant.now());
            repository.save(e);
            startedCounter.increment();
            context.getBean(IngestService.class).simulateIngest(runId, username, range);
            return runId;
        }
    }

    @Async("ingestExecutor")
    @Transactional
    public void simulateIngest(UUID runId, String username, @Nullable String range) {
        try (MDC.MDCCloseable m = MDC.putCloseable("run_id", runId.toString())) {
            activeRuns.incrementAndGet();
            try {
                Thread.sleep(delaySupplier.getAsLong());
                IngestRunEntity e = repository.findById(runId).orElseThrow();
                e.setStatus(IngestRunStatus.SUCCEEDED);
                e.setReportUri("s3://reports/ingest/%s/report.json".formatted(runId));
                e.setFinishedAt(Instant.now());
                repository.save(e);
                succeededCounter.increment();
                log.info("ingest succeeded");
            } catch (Exception ex) {
                IngestRunEntity e = repository.findById(runId).orElse(null);
                if (e != null) {
                    e.setStatus(IngestRunStatus.FAILED);
                    e.setError(ex.getMessage());
                    e.setFinishedAt(Instant.now());
                    repository.save(e);
                }
                failedCounter.increment();
                log.error("ingest failed", ex);
            } finally {
                activeRuns.decrementAndGet();
            }
        }
    }

    @Transactional(readOnly = true)
    public IngestStatusResponse getStatus(UUID runId) {
        IngestRunEntity e = repository.findById(runId).orElseThrow();
        return new IngestStatusResponse(e.getStatus().name(), e.getReportUri());
    }
}
