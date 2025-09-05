package com.chessapp.api.evaluations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/evaluations")
public class EvaluationsController {

    private static class Eval {
        String baseline;
        String candidate;
        String status = "queued";
        Instant createdAt = Instant.now();
        Eval(String baseline, String candidate) {
            this.baseline = baseline; this.candidate = candidate;
        }
    }

    private final Map<String, Eval> evals = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody Map<String, String> body) {
        String id = UUID.randomUUID().toString();
        evals.put(id, new Eval(body.get("baselineModelId"), body.get("candidateModelId")));
        return ResponseEntity.accepted().body(Map.of("evaluationId", id));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue="20") int limit, @RequestParam(defaultValue="0") int offset) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, Eval> e : evals.entrySet()) {
            Eval ev = e.getValue();
            items.add(Map.of(
                    "id", e.getKey(),
                    "baseline", ev.baseline,
                    "candidate", ev.candidate,
                    "status", ev.status,
                    "createdAt", ev.createdAt.toString()
            ));
        }
        return Map.of("items", items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        Eval ev = evals.get(id);
        if (ev == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> summary = Map.of(
                "baseline", ev.baseline,
                "candidate", ev.candidate
        );
        List<Map<String, Object>> series = List.of(Map.of("label", "dummy", "points", List.of()));
        return ResponseEntity.ok(Map.of(
                "status", ev.status,
                "summary", summary,
                "series", series
        ));
    }
}
