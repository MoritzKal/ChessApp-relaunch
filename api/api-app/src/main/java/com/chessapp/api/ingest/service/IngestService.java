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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class IngestService {

    private final PgnParser pgnParser;
    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final PositionRepository positionRepository;
    private final IngestRunRepository ingestRunRepository;
    private final MeterRegistry meterRegistry;

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
            if (offline) {
                String data = Files.readString(Path.of("fixtures/pgn/sample_10_games.pgn"));
                String[] parts = data.split("\r?\n\r?\n");
                List<Game> games = new ArrayList<>();
                List<Move> moves = new ArrayList<>();
                List<Position> positions = new ArrayList<>();
                for (String pgn : parts) {
                    if (pgn.trim().isEmpty()) continue;
                    PgnParser.ParsedGame parsed = pgnParser.parse(pgn.trim());
                    if (gameRepository.findByGameIdExt(parsed.gameIdExt()).isPresent()) {
                        skipped++;
                        continue;
                    }
                    UUID gameId = UUID.randomUUID();
                    Game g = new Game();
                    g.setId(gameId);
                    g.setUserId(UUID.randomUUID());
                    g.setGameIdExt(parsed.gameIdExt());
                    g.setEndTime(parsed.endTime());
                    g.setTimeControl(parsed.timeControl());
                    g.setResult(parsed.result());
                    g.setWhiteRating(parsed.whiteRating());
                    g.setBlackRating(parsed.blackRating());
                    g.setPgn(parsed.pgnRaw());
                    games.add(g);

                    for (PgnParser.ParsedMove m : parsed.moves()) {
                        Move mv = new Move();
                        mv.setId(UUID.randomUUID());
                        mv.setGameId(gameId);
                        mv.setPly(m.ply());
                        mv.setSan(m.san());
                        mv.setUci(m.uci());
                        mv.setColor(m.color());
                        moves.add(mv);
                    }
                    for (PgnParser.ParsedPosition p : parsed.positions()) {
                        Position pos = new Position();
                        pos.setId(UUID.randomUUID());
                        pos.setGameId(gameId);
                        pos.setPly(p.ply());
                        pos.setFen(p.fen());
                        pos.setSideToMove(p.sideToMove());
                        pos.setLegalMoves(p.legalMoves());
                        positions.add(pos);
                    }
                    gamesCount++;
                    movesCount += parsed.moves().size();
                    positionsCount += parsed.positions().size();
                }
                gameRepository.saveAll(games);
                moveRepository.saveAll(moves);
                positionRepository.saveAll(positions);
            }
            run.setStatus("succeeded");
            run.setGamesCount(gamesCount);
            run.setMovesCount(movesCount);
            run.setPositionsCount(positionsCount);
            run.setFinishedAt(Instant.now());
            ingestRunRepository.save(run);
            meterRegistry.counter("chs_ingest_games_total").increment(gamesCount);
            meterRegistry.counter("chs_ingest_positions_total").increment(positionsCount);
            log.info("event=ingest.completed games={} moves={} positions={} skipped={}"
                    , gamesCount, movesCount, positionsCount, skipped);
        } catch (Exception e) {
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
}
