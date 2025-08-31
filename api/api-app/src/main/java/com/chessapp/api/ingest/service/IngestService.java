package com.chessapp.api.ingest.service;

import com.chessapp.api.ingest.IngestRunStatus;
import com.chessapp.api.ingest.entity.IngestRunEntity;
import com.chessapp.api.ingest.repo.IngestRunRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class IngestService {

    private final IngestRunRepository repository;
    private final MeterRegistry meterRegistry;

    public IngestService(IngestRunRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    public UUID start(String username, String range) {
        UUID runId = UUID.randomUUID();
        IngestRunEntity run = new IngestRunEntity();
        run.setId(runId);
        run.setUsername(username);
        run.setRange(range);
        run.setStatus(IngestRunStatus.PENDING);
        run.setStartedAt(Instant.now());
        repository.save(run);
        runAsync(runId, username);
        return runId;
    }

    @Async("ingestExecutor")
    public void runAsync(UUID runId, String username) {
        MDC.put("run_id", runId.toString());
        try {
            meterRegistry.counter("chs_ingest_jobs_total", "username", username).increment();
            IngestRunEntity run = repository.findById(runId).orElseThrow();
            run.setStatus(IngestRunStatus.RUNNING);
            repository.save(run);
            // simulate work
            Thread.sleep(500);
            run.setStatus(IngestRunStatus.SUCCEEDED);
            run.setReportUri("s3://reports/ingest/" + runId + "/report.json");
            run.setFinishedAt(Instant.now());
            repository.save(run);
        } catch (InterruptedException e) {
            IngestRunEntity run = repository.findById(runId).orElse(null);
            if (run != null) {
                run.setStatus(IngestRunStatus.FAILED);
                run.setError(e.getMessage());
                run.setFinishedAt(Instant.now());
                repository.save(run);
            }
            Thread.currentThread().interrupt();
        } finally {
            MDC.remove("run_id");
        }
    }

    public IngestRunEntity get(UUID runId) {
        return repository.findById(runId).orElseThrow();
    }
}
