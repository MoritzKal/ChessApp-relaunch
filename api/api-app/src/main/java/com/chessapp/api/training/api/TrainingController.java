package com.chessapp.api.training.api;

import com.chessapp.api.training.service.TrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/{runId}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable UUID runId) {
        return ResponseEntity.ok(service.getStatus(runId));
    }
}
