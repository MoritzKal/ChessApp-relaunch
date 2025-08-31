package com.chessapp.api.ingest;

import com.chessapp.api.ingest.dto.IngestStatusResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final IngestRunRepository repository;
    private final Counter startedCounter;
    private final Counter succeededCounter;
    private final Counter failedCounter;
    private final AtomicInteger activeRuns;
    private final Supplier<Long> sleepSupplier;

    public IngestService(IngestRunRepository repository, MeterRegistry registry, Supplier<Long> ingestSleep) {
        this.repository = repository;
        this.startedCounter = Counter.builder("chs_ingest_runs_started_total").register(registry);
        this.succeededCounter = Counter.builder("chs_ingest_runs_succeeded_total").register(registry);
        this.failedCounter = Counter.builder("chs_ingest_runs_failed_total").register(registry);
        this.activeRuns = registry.gauge("chs_ingest_active_runs", new AtomicInteger(0));
        this.sleepSupplier = ingestSleep;
    }

    public UUID startRun(String username, @Nullable String range) {
        UUID runId = UUID.randomUUID();
        IngestRunEntity entity = new IngestRunEntity();
        entity.setId(runId);
        entity.setUsername(username);
        entity.setRange(range);
        entity.setStatus(IngestRunStatus.RUNNING);
        entity.setStartedAt(Instant.now());
        repository.save(entity);

        startedCounter.increment();
        activeRuns.incrementAndGet();

        try (MDC.MDCCloseable c = MDC.putCloseable("run_id", runId.toString())) {
            log.info("ingest requested username={} range={}", username, range);
        }

        simulateIngest(runId, username, range);
        return runId;
    }

    @Async
    void simulateIngest(UUID runId, String username, @Nullable String range) {
        try (MDC.MDCCloseable c = MDC.putCloseable("run_id", runId.toString())) {
            Thread.sleep(sleepSupplier.get());
            IngestRunEntity entity = repository.findById(runId).orElseThrow();
            entity.setStatus(IngestRunStatus.SUCCEEDED);
            entity.setReportUri("s3://reports/ingest/%s/report.json".formatted(runId));
            entity.setFinishedAt(Instant.now());
            repository.save(entity);
            succeededCounter.increment();
        } catch (Exception e) {
            IngestRunEntity entity = repository.findById(runId).orElse(null);
            if (entity != null) {
                entity.setStatus(IngestRunStatus.FAILED);
                entity.setError(e.getMessage());
                entity.setFinishedAt(Instant.now());
                repository.save(entity);
            }
            failedCounter.increment();
            log.error("ingest failed", e);
        } finally {
            activeRuns.decrementAndGet();
            MDC.remove("run_id");
        }
    }

    public IngestStatusResponse getStatus(UUID runId) {
        IngestRunEntity entity = repository.findById(runId).orElseThrow();
        return new IngestStatusResponse(entity.getStatus().name(), entity.getReportUri());
    }
}
