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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.chessapp.api.domain.entity.Platform;
import com.chessapp.api.domain.entity.TimeControlCategory;
import com.chessapp.api.domain.entity.User;
import com.chessapp.api.domain.repo.UserRepository;

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
    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;
    private final ChessComClient chessComClient;
    private final ArtifactWriter artifactWriter;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(IngestService.class);
    private final ThreadPoolTaskExecutor ingestExecutor;
    @Value("${ingest.offline.pgn-path:}")
    private String offlinePgnPath;
    private final Environment environment;

    public IngestService(PgnParser pgnParser,
                         GameRepository gameRepository,
                         MoveRepository moveRepository,
                         PositionRepository positionRepository,
                         IngestRunRepository ingestRunRepository,
                         UserRepository userRepository,
                         MeterRegistry meterRegistry,
                         ChessComClient chessComClient,
                         ArtifactWriter artifactWriter,
                         ObjectMapper objectMapper,
                        @Qualifier("ingestExecutor") ThreadPoolTaskExecutor ingestExecutor,
                         Environment environment ){
        log.info("IngestService wired (beanClass={})", this.getClass());
        this.pgnParser = pgnParser;
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.positionRepository = positionRepository;
        this.ingestRunRepository = ingestRunRepository;
        this.userRepository = userRepository;
        this.meterRegistry = meterRegistry;
        this.chessComClient = chessComClient;
        this.artifactWriter = artifactWriter;
        this.objectMapper = objectMapper;
        this.ingestExecutor = ingestExecutor;
        this.environment = environment;
        log.info("IngestService wired (beanClass={})", this.getClass());
    }

    public void enqueueIngest(UUID runId, String username, YearMonth from, YearMonth to, boolean offline) {
        ingestExecutor.execute(() -> {
            try {
                startIngest(runId, username, from, to, offline); // synchroner Body
            } catch (Exception e) {
                log.error("event=ingest.failed error={}", e.getMessage(), e);
                try {
                    var run = ingestRunRepository.findById(runId).orElse(null);
                    if (run != null) {
                        run.setStatus("failed");
                        run.setError(e.getMessage());
                        run.setFinishedAt(Instant.now());
                        ingestRunRepository.saveAndFlush(run);
                    }
                } catch (Exception ignore) {}
            }
        });
    }

    //@Async("ingestExecutor")
    @Transactional
    public void startIngest(UUID runId, String username, YearMonth from, YearMonth to, boolean offline) {
        UUID userId = resolveUserId(username);
        log.info("event=ingest.started thread={}", Thread.currentThread().getName());
        MDC.put("run_id", runId.toString());
        MDC.put("username", username);
        MDC.put("component", "ingest");
        log.info("event=ingest.started");
        meterRegistry.counter("chs_ingest_jobs_total").increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        IngestRun run = ingestRunRepository.findById(runId).orElseThrow();
        run.setStatus("running");
        run.setStartedAt(Instant.now());
        ingestRunRepository.saveAndFlush(run);
        log.info("event=ingest.status_updated status=running run_id={} username={}", runId, username);

        int gamesCount = 0;
        long movesCount = 0;
        long positionsCount = 0;
        int skipped = 0;

        try {
            if (offline) {
                String data = loadOfflinePgn(); // <— NEU

                List<PgnParser.ParsedGame> parsedGames = pgnParser.parseManyFromConcatPgn(data);

                List<Game> games = new ArrayList<>();
                List<Move> moves = new ArrayList<>();
                List<Position> positions = new ArrayList<>();

                for (PgnParser.ParsedGame parsed : parsedGames) {
                    if (parsed == null) continue;
                    if (parsed.gameIdExt() != null) {
                        var platform = Platform.CHESS_COM;
                        if (gameRepository.findByPlatformAndGameIdExt(platform, parsed.gameIdExt()).isPresent()) {
                            io.micrometer.core.instrument.Counter.builder("chs_ingest_skipped_total")
                                .description("Number of games skipped due to duplicate (platform + game_id_ext)")
                                .tag("username", username)
                                .register(meterRegistry)
                                .increment();
                            log.info("event=ingest.duplicate_skipped platform={} game_id_ext={} run_id={} username={}",
                                    platform.name(), parsed.gameIdExt(), runId, username);
                            skipped++;
                            continue;
                        }
                    }
                    UUID gameId = UUID.randomUUID();

                    Game g = new Game();
                    g.setId(gameId);
                    g.setUserId(userId);
                    g.setGameIdExt(parsed.gameIdExt());
                    g.setEndTime(parsed.endTime());
                    g.setTimeCategory(deriveTimeCategory(parsed.timeControl()));
                    g.setTimeControl(parsed.timeControl());
                    g.setResult(mapPgnResult(parsed.result()));
                    g.setWhiteRating(parsed.whiteRating());
                    g.setBlackRating(parsed.blackRating());
                    g.setPgn(parsed.pgnRaw());
                    g.setPlatform(Platform.CHESS_COM);
                    games.add(g);

                    for (PgnParser.ParsedMove m : parsed.moves()) {
                        Move mv = new Move();
                        mv.setId(UUID.randomUUID());
                        mv.setGameId(gameId);
                        mv.setPly(m.ply());
                        mv.setSan(m.san());
                        mv.setUci(m.uci());
                        mv.setColor(mapColor(m.color()));
                        moves.add(mv);
                    }
                    for (PgnParser.ParsedPosition p : parsed.positions()) {
                        Position pos = new Position();
                        pos.setId(UUID.randomUUID());
                        pos.setGameId(gameId);
                        pos.setPly(p.ply());
                        pos.setFen(p.fen());
                        pos.setSideToMove(mapColor(p.sideToMove()));
                        positions.add(pos);
                    }

                    int sumLegal = parsed.positions().stream().mapToInt(PgnParser.ParsedPosition::legalMovesCount).sum();
                    meterRegistry.counter("chs_positions_legal_moves_total", "username", username).increment(sumLegal);

                    gamesCount++;
                    movesCount += parsed.moves().size();
                    positionsCount += parsed.positions().size();
                }

                if (!games.isEmpty()) gameRepository.saveAll(games);
                if (!moves.isEmpty()) moveRepository.saveAll(moves);
                if (!positions.isEmpty()) positionRepository.saveAll(positions);

                run.setStatus("succeeded");
                run.setGamesCount(gamesCount);
                run.setMovesCount(movesCount);
                run.setPositionsCount(positionsCount);
                Instant finishedAt = Instant.now();
                run.setFinishedAt(finishedAt);

                long durationMs = java.time.Duration.between(run.getStartedAt(), finishedAt).toMillis();
                Map<String, Object> report = Map.of(
                        "runId", runId.toString(),
                        "username", username,
                        "from", from != null ? from.toString() : null,
                        "to", to != null ? to.toString() : null,
                        "counts", Map.of(
                                "games", gamesCount,
                                "moves", movesCount,
                                "positions", positionsCount,
                                "skipped", skipped
                        ),
                        "durationMs", durationMs,
                        "startedAt", run.getStartedAt(),
                        "finishedAt", finishedAt
                );
                String reportUri = artifactWriter.putReport(runId.toString(), report);
                run.setReportUri(reportUri);
                ingestRunRepository.save(run);
                log.info("event=ingest.report_written run_id={} username={} report_uri={}", runId, username, reportUri);

                meterRegistry.counter("chs_ingest_games_total").increment(gamesCount);
                meterRegistry.counter("chs_ingest_positions_total").increment(positionsCount);
                log.info("event=ingest.completed run_id={} username={} games={} moves={} positions={} skipped={} report_uri={}",
                        runId, username, gamesCount, movesCount, positionsCount, skipped, reportUri);
            } else {
                // 1) Archive holen und auf [from..to] filtern
                var archives = chessComClient.listArchives(username).blockOptional().orElseGet(java.util.List::of);
                var want = new java.util.ArrayList<java.time.YearMonth>();
                for (var a : archives) {
                    // a ~ ".../YYYY/MM"
                    var parts = a.split("/");
                    if (parts.length >= 2) {
                        int y = Integer.parseInt(parts[parts.length-2]);
                        int m = Integer.parseInt(parts[parts.length-1]);
                        var ym = java.time.YearMonth.of(y,m);
                        if ((ym.equals(from) || ym.isAfter(from)) && (ym.equals(to) || ym.isBefore(to))) {
                            want.add(ym);
                        }
                    }
                }
                // Artefakt: archives.json (gefilterte Monate)
                artifactWriter.putJsonToLogs(runId.toString(), "archives.json", want);

                // 2) Für jeden Monat alle PGNs einsammeln, zusammen parsen, persistieren
                for (var ym : want) {
                    var monthGames = chessComClient.fetchMonth(username, ym).collectList().block();
                    if (monthGames == null || monthGames.isEmpty()) continue;

                    StringBuilder sb = new StringBuilder();
                    for (var node : monthGames) {
                        var pgnNode = node.get("pgn");
                        if (pgnNode != null && !pgnNode.isNull()) {
                            sb.append(pgnNode.asText()).append("\n\n");
                        }
                    }
                    var parsedGames = pgnParser.parseManyFromConcatPgn(sb.toString());

                    java.util.List<com.chessapp.api.domain.entity.Game> games = new java.util.ArrayList<>();
                    java.util.List<com.chessapp.api.domain.entity.Move> moves = new java.util.ArrayList<>();
                    java.util.List<com.chessapp.api.domain.entity.Position> positions = new java.util.ArrayList<>();

                    for (var parsed : parsedGames) {
                        if (parsed == null) continue;
                        if (parsed.gameIdExt() != null) {
                            var platform = Platform.CHESS_COM;
                            if (gameRepository.findByPlatformAndGameIdExt(platform, parsed.gameIdExt()).isPresent()) {
                                io.micrometer.core.instrument.Counter.builder("chs_ingest_skipped_total")
                                    .description("Number of games skipped due to duplicate (platform + game_id_ext)")
                                    .tag("username", username)
                                    .register(meterRegistry)
                                    .increment();
                                log.info("event=ingest.duplicate_skipped platform={} game_id_ext={} run_id={} username={}",
                                        platform.name(), parsed.gameIdExt(), runId, username);
                                skipped++; continue;
                            }
                        }
                        var gameId = java.util.UUID.randomUUID();
                        var g = new com.chessapp.api.domain.entity.Game();
                        g.setId(gameId);
                        g.setUserId(userId);
                        g.setGameIdExt(parsed.gameIdExt());
                        g.setEndTime(parsed.endTime());
                        g.setTimeCategory(deriveTimeCategory(parsed.timeControl()));
                        g.setTimeControl(parsed.timeControl());
                        g.setWhiteRating(parsed.whiteRating());
                        g.setBlackRating(parsed.blackRating());
                        g.setPgn(parsed.pgnRaw());
                        g.setResult(mapPgnResult(parsed.result()));
                        g.setPlatform(Platform.CHESS_COM);
                        games.add(g);

                        for (var m : parsed.moves()) {
                            var mv = new com.chessapp.api.domain.entity.Move();
                            mv.setId(java.util.UUID.randomUUID());
                            mv.setGameId(gameId);
                            mv.setPly(m.ply());
                            mv.setSan(""); // SAN im MVP leer
                            mv.setUci(m.uci());
                            mv.setColor(mapColor(m.color()));
                            moves.add(mv);
                        }
                        for (var p : parsed.positions()) {
                            var pos = new com.chessapp.api.domain.entity.Position();
                            pos.setId(java.util.UUID.randomUUID());
                            pos.setGameId(gameId);
                            pos.setPly(p.ply());
                            pos.setFen(p.fen());
                            pos.setSideToMove(mapColor(p.sideToMove()));
                            positions.add(pos);
                        }
                        gamesCount++;
                        movesCount += parsed.moves().size();
                        positionsCount += parsed.positions().size();
                    }
                    if (!games.isEmpty()) gameRepository.saveAll(games);
                    if (!moves.isEmpty()) moveRepository.saveAll(moves);
                    if (!positions.isEmpty()) positionRepository.saveAll(positions);
                }

                // 3) Abschluss-Report in S3 schreiben & in ingest_runs referenzieren
                var report = java.util.Map.of(
                    "runId", runId.toString(),
                    "username", username,
                    "from", from.toString(),
                    "to", to.toString(),
                    "counts", java.util.Map.of(
                        "games", gamesCount,
                        "moves", movesCount,
                        "positions", positionsCount,
                        "skipped", skipped
                    ),
                    "startedAt", run.getStartedAt(),
                    "finishedAt", java.time.Instant.now()
                );
                String reportUri = artifactWriter.putReport(runId.toString(), report);
                run.setReportUri(reportUri);
                log.info("event=ingest.status_updated status=succeeded run_id={} username={}", runId, username);
                run.setStatus("succeeded");
                run.setGamesCount(gamesCount);
                run.setMovesCount(movesCount);
                run.setPositionsCount(positionsCount);
                run.setFinishedAt(Instant.now());
                ingestRunRepository.saveAndFlush(run);
                meterRegistry.counter("chs_ingest_games_total").increment(gamesCount);meterRegistry.counter("chs_ingest_positions_total").increment(positionsCount);
                log.info("event=ingest.completed mode=online report_uri={} games={} moves={} positions={} skipped={}",
                        reportUri, gamesCount, movesCount, positionsCount, skipped);
            }
        } catch (Exception e) {
            log.error("event=ingest.failed error={}", e.getMessage(), e);
            run.setStatus("failed");
            run.setError(e.getMessage());
            run.setFinishedAt(Instant.now());
            ingestRunRepository.saveAndFlush(run);
            log.info("event=ingest.status_updated status=failed run_id={} username={}", runId, username);
        } finally {
            sample.stop(io.micrometer.core.instrument.Timer.builder("chs_ingest_duration_seconds")
                    .tag("application","api").tag("component","ingest").tag("username", username)
                    .register(meterRegistry));
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


    private String loadOfflinePgn() throws Exception {
        // codex-profile enhancement: allow override via property "ingest.offline.pgn-path"
        // Only takes effect if property is present (e.g., in application-codex.yml). Otherwise fallback to legacy logic.
        String configured = (offlinePgnPath != null) ? offlinePgnPath.trim() : "";
        if (!configured.isEmpty()) {
            if (configured.startsWith("classpath:")) {
                String cp = configured.substring("classpath:".length());
                if (cp.startsWith("/")) cp = cp.substring(1);
                ClassPathResource res = new ClassPathResource(cp);
                if (res.exists()) {
                    try (InputStream is = res.getInputStream()) {
                        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                throw new IllegalStateException("Configured offline PGN not found on classpath: " + configured);
            } else {
                Path fs = Path.of(configured);
                if (Files.exists(fs)) {
                    return Files.readString(fs, StandardCharsets.UTF_8);
                }
                throw new IllegalStateException("Configured offline PGN file not found: " + configured);
            }
        }

        // Fallback: legacy default locations
        ClassPathResource res = new ClassPathResource("fixtures/pgn/sample_10_games.pgn");
        if (res.exists()) {
            try (InputStream is = res.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        Path fs = Path.of("fixtures/pgn/sample_10_games.pgn");
        if (Files.exists(fs)) {
            return Files.readString(fs, StandardCharsets.UTF_8);
        }
        throw new IllegalStateException("Offline PGN not found (classpath or filesystem).");
    }

    private TimeControlCategory deriveTimeCategory(String tc) {
        try {
            if (tc == null || tc.isBlank()) return TimeControlCategory.RAPID; // defensiver Default
            String[] p = tc.split("\\+");
            int base = Integer.parseInt(p[0]); // Sekunden
            if (base < 180)  return TimeControlCategory.BULLET;
            if (base < 480)  return TimeControlCategory.BLITZ;
            if (base < 1500) return TimeControlCategory.RAPID;
            return TimeControlCategory.CLASSICAL;
        } catch (Exception e) {
            try { return TimeControlCategory.valueOf("RAPID"); } catch (Exception ignore) { return TimeControlCategory.values()[0]; }
        }
    }

    private UUID resolveUserId(String username) {
        return userRepository.findByChessUsername(username)
                .map(com.chessapp.api.domain.entity.User::getId)
                .orElseGet(() -> {
                    var u = new com.chessapp.api.domain.entity.User();
                    u.setId(java.util.UUID.randomUUID());
                    u.setChessUsername(username);
                    u.setCreatedAt(java.time.Instant.now()); // <— WICHTIG
                    userRepository.saveAndFlush(u);
                    return u.getId();
                });
    }
}


