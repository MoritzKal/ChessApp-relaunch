package com.chessapp.api.training.service;

import com.chessapp.api.domain.entity.TrainingRun;
import com.chessapp.api.domain.entity.TrainingStatus;
import com.chessapp.api.domain.repo.TrainingRunRepository;
import com.chessapp.api.training.api.TrainingStartRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class TrainingService {

    private final TrainingRunRepository repo;
    private final MlClient mlClient;
    private final Counter runsTotal;

    public TrainingService(TrainingRunRepository repo, MlClient mlClient, MeterRegistry registry) {
        this.repo = repo;
        this.mlClient = mlClient;
        this.runsTotal = Counter.builder("chs_training_runs_total").register(registry);
    }

    @Transactional
    public UUID startTraining(TrainingStartRequest req) {
        UUID runId = UUID.randomUUID();
        try (MDC.MDCCloseable r = MDC.putCloseable("run_id", runId.toString())) {
            TrainingRun tr = new TrainingRun();
            tr.setId(runId);
            tr.setDatasetId(req.datasetId());
            tr.setParams(Optional.ofNullable(req.params()).orElseGet(Map::of));
            tr.setStatus(TrainingStatus.QUEUED);
            repo.save(tr);

            mlClient.postTrain(runId, req.datasetId(), req.params());

            tr.setStatus(TrainingStatus.RUNNING);
            tr.setStartedAt(Instant.now());
            repo.save(tr);

            runsTotal.increment();
            return runId;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatus(UUID runId) {
        TrainingRun tr = repo.findById(runId).orElse(null);
        Map<String, Object> ml = Map.of();
        try { ml = mlClient.getRun(runId); } catch (Exception ignored) {}

        Map<String, Object> out = new LinkedHashMap<>();
        if (tr != null) {
            out.put("runId", tr.getId().toString());
            out.put("status", tr.getStatus().name().toLowerCase());
            out.put("startedAt", tr.getStartedAt());
            out.put("finishedAt", tr.getFinishedAt());
            out.put("metrics", tr.getMetrics());
            out.put("artifactUris", Optional.ofNullable(tr.getLogsUri()).map(u -> Map.of("logs", u)).orElseGet(Map::of));
        }
        if (ml.get("status") != null) out.put("status", String.valueOf(ml.get("status")));
        if (ml.get("metrics") instanceof Map<?,?> m) out.put("metrics", m);
        if (ml.get("artifactUris") instanceof Map<?,?> a) out.put("artifactUris", a);
        if (ml.get("startedAt") != null) out.put("startedAt", ml.get("startedAt"));
        if (ml.get("finishedAt") != null) out.put("finishedAt", ml.get("finishedAt"));
        return out;
    }
}
