package com.chessapp.api.play;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/play")
public class PlayController {

    private record Move(int ply, String san, String uci, String side, long tMs) {}
    private static class Game {
        String fen;
        Instant startedAt;
        List<Move> moves = new ArrayList<>();
        Game(String fen) { this.fen = fen; this.startedAt = Instant.now(); }
    }

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    @PostMapping("/new")
    public Map<String, Object> start() {
        String id = UUID.randomUUID().toString();
        games.put(id, new Game("startpos"));
        return Map.of("gameId", id, "startedAt", games.get(id).startedAt.toString());
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<Map<String, Object>> move(@PathVariable String id, @RequestBody Map<String, String> body) {
        Game g = games.get(id);
        if (g == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
        }
        String uci = body.getOrDefault("uci", "");
        int ply = g.moves.size() + 1;
        String side = ply % 2 == 1 ? "w" : "b";
        g.moves.add(new Move(ply, uci, uci, side, 0));
        return ResponseEntity.ok(Map.of("ok", true, "fen", g.fen));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        Game g = games.get(id);
        if (g == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
        }
        return ResponseEntity.ok(Map.of(
                "fen", g.fen,
                "moves", g.moves
        ));
    }
}
