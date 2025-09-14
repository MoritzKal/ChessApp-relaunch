package com.chessapp.api.training.api;

import com.chessapp.api.common.dto.CountDto;
import com.chessapp.api.training.api.dto.ArtifactDto;
import com.chessapp.api.training.api.dto.ArtifactListDto;
import com.chessapp.api.training.api.dto.TrainingListDto;
import com.chessapp.api.training.service.TrainingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/trainings")
public class TrainingController {

    private static final Logger log = LoggerFactory.getLogger(TrainingController.class);

    private final TrainingService service;
    public TrainingController(TrainingService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Map<String, Object>> start(@Valid @RequestBody TrainingStartRequest req) {
        UUID runId = service.startTraining(req);
        log.info("audit.training.start run_id={} dataset_id={} model_id={}", runId, req.datasetId(), req.modelId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("runId", runId.toString(), "status", "queued"));
    }

    @GetMapping
    public TrainingListDto list(@RequestParam(required = false) String status,
                                @RequestParam(defaultValue = "20") int limit,
                                @RequestParam(defaultValue = "0") int offset) {
        if (limit < 1) limit = 1;
        if (limit > 100) limit = 100;
        return new TrainingListDto(service.list(status, limit, offset));
    }

    @GetMapping("/count")
    public CountDto count(@RequestParam(required = false) String status) {
        return new CountDto(service.count(status));
    }

    @GetMapping("/{runId}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable UUID runId) {
        return ResponseEntity.ok(service.getStatus(runId));
    }

    @GetMapping("/{runId}/artifacts")
    public ArtifactListDto artifacts(@PathVariable UUID runId) {
        Map<String, Object> st = service.getStatus(runId);
        Object a = st.get("artifactUris");
        java.util.List<ArtifactDto> items = new java.util.ArrayList<>();
        if (a instanceof Map<?,?> map) {
            for (var e : map.entrySet()) {
                String name = String.valueOf(e.getKey());
                String uri = String.valueOf(e.getValue());
                items.add(new ArtifactDto(name, 0L, uri));
            }
        }
        return new ArtifactListDto(items);
    }

    @GetMapping("/{runId}/hyperparams")
    public ResponseEntity<Map<String, Object>> hyperparams(@PathVariable UUID runId) {
        // Return stored params from the DB entity if available
        try {
            java.util.Map<String, Object> st = service.getParams(runId);
            return ResponseEntity.ok(st);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of());
        }
    }

    @PostMapping("/{runId}/control")
    public ResponseEntity<Map<String, Object>> control(@PathVariable UUID runId,
                                                       @RequestBody Map<String, String> body) {
        String action = body.get("action");
        if (!"pause".equals(action) && !"stop".equals(action)) {
            return ResponseEntity.badRequest().build();
        }
        log.info("audit.training.control run_id={} action={}", runId, action);
        return ResponseEntity.accepted().body(Map.of("queued", true));
    }
}
