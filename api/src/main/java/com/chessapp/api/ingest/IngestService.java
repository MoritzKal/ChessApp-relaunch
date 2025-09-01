package com.chessapp.api.ingest;

import com.chessapp.api.ingest.dto.IngestStatusResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final Timer jobTimer;
    private final Supplier<Long> sleepSupplier;
    private final IngestService self;

    public IngestService(IngestRunRepository repository, MeterRegistry registry, Supplier<Long> ingestSleep,
                         @Lazy IngestService self) {
        this.repository = repository;
        this.startedCounter = Counter.builder("chs_ingest_runs_started_total").register(registry);
        this.succeededCounter = Counter.builder("chs_ingest_runs_succeeded_total").register(registry);
        this.failedCounter = Counter.builder("chs_ingest_runs_failed_total").register(registry);
        this.activeRuns = registry.gauge("chs_ingest_active_runs", new AtomicInteger(0));
        this.jobTimer = Timer.builder("chs_ingest_job_duration_seconds").register(registry);
        this.sleepSupplier = ingestSleep;
        this.self = self;
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

        self.simulateIngest(runId, username, range);
        return runId;
    }

    @Async("ingestExecutor")
    void simulateIngest(UUID runId, String username, @Nullable String range) {
        Instant finish = null;
        try (MDC.MDCCloseable c = MDC.putCloseable("run_id", runId.toString())) {
            Thread.sleep(sleepSupplier.get());
            IngestRunEntity entity = repository.findById(runId).orElseThrow();
            entity.setStatus(IngestRunStatus.SUCCEEDED);
            entity.setReportUri("s3://reports/ingest/%s/report.json".formatted(runId));
            finish = Instant.now();
            entity.setFinishedAt(finish);
            repository.save(entity);
            succeededCounter.increment();
            jobTimer.record(Duration.between(entity.getStartedAt(), finish));
        } catch (Exception e) {
            IngestRunEntity entity = repository.findById(runId).orElse(null);
            finish = Instant.now();
            if (entity != null) {
                entity.setStatus(IngestRunStatus.FAILED);
                entity.setError(e.getMessage());
                entity.setFinishedAt(finish);
                repository.save(entity);
                jobTimer.record(Duration.between(entity.getStartedAt(), finish));
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
