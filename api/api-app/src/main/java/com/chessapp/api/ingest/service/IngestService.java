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
import org.springframework.stereotype.Service;

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
    private final Timer.Builder durationTimer;
    private final AtomicInteger activeGauge;
    private final IngestService self;

    public IngestService(IngestRunRepository repository, MeterRegistry meterRegistry,
                         @Lazy IngestService self) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
        this.starts = meterRegistry.counter("chs_ingest_starts_total");
        this.success = meterRegistry.counter("chs_ingest_success_total");
        this.failed = meterRegistry.counter("chs_ingest_failed_total");
        this.durationTimer = Timer.builder("chs_ingest_duration_seconds")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95);
        this.activeGauge = meterRegistry.gauge("chs_ingest_active", new AtomicInteger());
        this.self = self;
    }

    /**
     * Starts a new ingest run and schedules async execution.
     *
     * @return run identifier
     */
    public UUID start() {
        UUID runId = UUID.randomUUID();
        IngestRunEntity run = new IngestRunEntity();
        run.setRunId(runId);
        run.setStatus("PENDING");
        repository.save(run);
        starts.increment();
        MDC.put("run_id", runId.toString());
        self.execute(runId);
        MDC.remove("run_id");
        return runId;
    }

    /**
     * Asynchronous execution of an ingest run.
     */
    @Async("ingestExecutor")
    public void execute(UUID runId) {
        activeGauge.incrementAndGet();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            update(runId, "RUNNING", null);
            log.info("ingest run {} running", runId);
            Thread.sleep(1000); // simulate work
            String report = "s3://reports/ingest/" + runId + "/report.json";
            update(runId, "SUCCEEDED", report);
            success.increment();
            sample.stop(durationTimer.tag("outcome", "success").register(meterRegistry));
            log.info("ingest run {} succeeded", runId);
        } catch (Exception e) {
            update(runId, "FAILED", null);
            failed.increment();
            sample.stop(durationTimer.tag("outcome", "failed").register(meterRegistry));
            log.error("ingest run {} failed: {}", runId, e.getMessage());
        } finally {
            activeGauge.decrementAndGet();
        }
    }

    private void update(UUID runId, String status, String reportUri) {
        IngestRunEntity run = repository.findById(runId).orElseThrow();
        run.setStatus(status);
        if (reportUri != null) {
            run.setReportUri(reportUri);
        }
        repository.save(run);
    }
}
