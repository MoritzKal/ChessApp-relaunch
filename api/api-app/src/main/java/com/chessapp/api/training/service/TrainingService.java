package com.chessapp.api.training.service;

import com.chessapp.api.domain.entity.TrainingRun;
import com.chessapp.api.domain.entity.TrainingStatus;
import com.chessapp.api.domain.repo.TrainingRunRepository;
import com.chessapp.api.training.api.TrainingStartRequest;
import com.chessapp.api.training.api.dto.TrainingItemDto;
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
            tr.setModelId(req.modelId());

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("datasetVersion", req.datasetVersion());
            params.put("epochs", req.epochs());
            params.put("batchSize", req.batchSize());
            params.put("learningRate", req.learningRate());
            params.put("optimizer", req.optimizer());
            params.put("seed", req.seed());
            params.put("notes", req.notes());
            params.put("useGPU", req.useGPU());
            params.put("priority", req.priority());
            tr.setParams(params);

            tr.setStatus(TrainingStatus.QUEUED);
            repo.save(tr);

            mlClient.postTrain(runId, req.datasetId(), params);

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
        out.putIfAbsent("etaSec", null);
        out.putIfAbsent("step", 0);
        out.putIfAbsent("epoch", 0);
        out.putIfAbsent("progress", 0.0);
        if (ml.get("updatedAt") != null) {
            out.put("updatedAt", ml.get("updatedAt"));
        } else if (tr != null) {
            Instant upd = tr.getFinishedAt() != null ? tr.getFinishedAt() : Instant.now();
            out.put("updatedAt", upd.toString());
        }
        out.putIfAbsent("updatedAt", Instant.now().toString());
        return out;
    }

    @Transactional(readOnly = true)
    public List<TrainingItemDto> list(String status, int limit, int offset) {
        var page = org.springframework.data.domain.PageRequest.of(offset / limit, limit);
        List<TrainingRun> runs;
        if (status != null) {
            TrainingStatus ts = mapStatus(status);
            runs = repo.findAllByStatusOrderByStartedAtDesc(ts, page);
        } else {
            runs = repo.findAllByOrderByStartedAtDesc(page);
        }
        return runs.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long count(String status) {
        if (status != null) {
            TrainingStatus ts = mapStatus(status);
            return repo.countByStatus(ts);
        }
        return repo.count();
    }

    private TrainingStatus mapStatus(String status) {
        return switch (status) {
            case "active" -> TrainingStatus.RUNNING;
            case "finished" -> TrainingStatus.SUCCEEDED;
            case "failed" -> TrainingStatus.FAILED;
            default -> TrainingStatus.RUNNING;
        };
    }

    private TrainingItemDto toDto(TrainingRun tr) {
        Instant upd = tr.getFinishedAt() != null ? tr.getFinishedAt() : Instant.now();
        return new TrainingItemDto(
                tr.getId().toString(),
                tr.getStatus() != null ? tr.getStatus().name().toLowerCase() : "unknown",
                0.0,
                upd.toString()
        );
    }
}
