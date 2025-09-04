package com.chessapp.api.training.api;

import com.chessapp.api.common.dto.CountDto;
import com.chessapp.api.training.api.dto.ArtifactListDto;
import com.chessapp.api.training.api.dto.TrainingListDto;
import com.chessapp.api.training.service.TrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/trainings")
public class TrainingController {

    private final TrainingService service;
    public TrainingController(TrainingService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Map<String, Object>> start(@RequestBody TrainingStartRequest req) {
        UUID runId = service.startTraining(req);
        return ResponseEntity.accepted().body(Map.of("runId", runId.toString()));
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
        return new ArtifactListDto(List.of());
    }

    @PostMapping("/{runId}/control")
    public ResponseEntity<Map<String, Object>> control(@PathVariable UUID runId,
                                                       @RequestBody Map<String, String> body) {
        return ResponseEntity.accepted().body(Map.of("queued", true));
    }
}
