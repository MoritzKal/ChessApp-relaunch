package com.chessapp.api.ingest.service;

import com.chessapp.api.domain.entity.Game;
import com.chessapp.api.domain.entity.Move;
import com.chessapp.api.domain.entity.Position;
import com.chessapp.api.domain.repo.GameRepository;
import com.chessapp.api.domain.repo.MoveRepository;
import com.chessapp.api.domain.repo.PositionRepository;
import com.chessapp.api.ingest.entity.IngestRun;
import com.chessapp.api.ingest.repo.IngestRunRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.chessapp.api.domain.entity.Color;
import com.chessapp.api.domain.entity.GameResult;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;

@Service
public class IngestService {

    private final PgnParser pgnParser;
    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final PositionRepository positionRepository;
    private final IngestRunRepository ingestRunRepository;
    private final MeterRegistry meterRegistry;
    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    public IngestService(PgnParser pgnParser,
                         GameRepository gameRepository,
                         MoveRepository moveRepository,
                         PositionRepository positionRepository,
                         IngestRunRepository ingestRunRepository,
                         MeterRegistry meterRegistry) {
        this.pgnParser = pgnParser;
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.positionRepository = positionRepository;
        this.ingestRunRepository = ingestRunRepository;
        this.meterRegistry = meterRegistry;
    }

    @Async("ingestExecutor")
    @Transactional
    public void startIngest(UUID runId, String username, YearMonth from, YearMonth to, boolean offline) {
        MDC.put("run_id", runId.toString());
        MDC.put("username", username);
        MDC.put("component", "ingest");
        log.info("event=ingest.started");
        meterRegistry.counter("chs_ingest_jobs_total").increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        IngestRun run = ingestRunRepository.findById(runId).orElseThrow();
        run.setStatus("running");
        run.setStartedAt(Instant.now());
        ingestRunRepository.save(run);

        int gamesCount = 0;
        long movesCount = 0;
        long positionsCount = 0;
        int skipped = 0;

        try {
            if(offline) {
                String data = Files.readString(Path.of("fixtures/pgn/sample_10_games.pgn"));
                // Neu: in einem Rutsch alle Partien parsen (kompatibel zu chesslib 1.3.4)
                List<PgnParser.ParsedGame> parsedGames = pgnParser.parseManyFromConcatPgn(data);
                List<Game> games = new ArrayList<>();
                List<Move> moves = new ArrayList<>();
                List<Position> positions = new ArrayList<>();
                for (PgnParser.ParsedGame parsed : parsedGames) {
                    if (parsed == null) continue;
                    if (parsed.gameIdExt() != null && gameRepository.findByGameIdExt(parsed.gameIdExt()).isPresent()) {
                        skipped++;
                        continue;
                    }
                    UUID gameId = UUID.randomUUID();
                    Game g = new Game();
                    g.setId(gameId);
                    g.setUserId(UUID.randomUUID()); // MVP: Default-User, später via Mapping M3NG00S3
                    g.setGameIdExt(parsed.gameIdExt());
                    g.setEndTime(parsed.endTime());
                    g.setTimeControl(parsed.timeControl());
                    g.setResult(mapPgnResult(parsed.result()));
                    g.setWhiteRating(parsed.whiteRating());
                    g.setBlackRating(parsed.blackRating());
                    g.setPgn(parsed.pgnRaw());
                    games.add(g);
                    for (PgnParser.ParsedMove m : parsed.moves()) {
                        Move mv = new Move();
                        mv.setId(UUID.randomUUID());
                        mv.setGameId(gameId);
                        mv.setPly(m.ply());
                        // SAN liefern wir im MVP nicht – leerer String vermeidet NOT NULL-Probleme
                        mv.setSan("");
                        mv.setUci(m.uci());
                        mv.setColor(mapColor(m.color()));
                        moves.add(mv);
                    }
                    for (PgnParser.ParsedPosition p : parsed.positions()) {
                        Position pos = new Position();
                        pos.setId(UUID.randomUUID());
                        pos.setGameId(gameId);
                        pos.setPly(p.ply());
                        pos.setFen(p.fen());              // NOT NULL gemäß V2
                        pos.setSideToMove(mapColor(p.sideToMove()));
                        // legalMoves wird im MVP nicht befüllt; falls Spalte NOT NULL wäre -> "[]"
                        // pos.setLegalMoves("[]");
                        positions.add(pos);
                    }
                    gamesCount++;
                    movesCount += parsed.moves().size();
                    positionsCount += parsed.positions().size();
                }
                if (!games.isEmpty()) gameRepository.saveAll(games);
                if (!moves.isEmpty()) moveRepository.saveAll(moves);
                if (!positions.isEmpty()) positionRepository.saveAll(positions);
            }} catch (Exception e) {
            log.error("event=ingest.failed error={}", e.getMessage(), e);
            run.setStatus("failed");
            run.setError(e.getMessage());
            run.setFinishedAt(Instant.now());
            ingestRunRepository.save(run);
        } finally {
            sample.stop(meterRegistry.timer("chs_ingest_duration_seconds"));
            MDC.clear();
        }
    }


    private GameResult mapPgnResult(String pgnResult) {
        String s = (pgnResult == null) ? "" : pgnResult.trim();
        String[] candidates;
        switch (s) {
            case "1-0":
                candidates = new String[] {"WHITE", "WHITE_WIN", "WHITEWON", "WHITE_WON", "WHITE_WINS"};
                break;
            case "0-1":
                candidates = new String[] {"BLACK", "BLACK_WIN", "BLACKWON", "BLACK_WON", "BLACK_WINS"};
                break;
            case "1/2-1/2":
                candidates = new String[] {"DRAW", "REMIS", "TIE"};
                break;
            case "*":
            default:
                candidates = new String[] {"UNKNOWN", "ONGOING", "UNDECIDED", "ABORTED"};
        }
        for (String c : candidates) {
            try { return GameResult.valueOf(c); } catch (IllegalArgumentException ignored) {}
        }
        // Fallback: erstes Enum, um niemals null zu schreiben
        return GameResult.values()[0];
    }

    private Color mapColor(String c) {
        if (c == null) return Color.WHITE; // harmloser Default
        String s = c.trim().toUpperCase();
        try { return Color.valueOf(s); } catch (IllegalArgumentException ignored) {}
        if (s.equals("W")) return Color.WHITE;
        if (s.equals("B")) return Color.BLACK;
        return Color.WHITE;
    }
}


