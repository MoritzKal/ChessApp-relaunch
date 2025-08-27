package com.chessapp.api.ingest.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.chessapp.api.domain.entity.Game;
import com.chessapp.api.domain.entity.Move;
import com.chessapp.api.domain.entity.Position;
import com.chessapp.api.domain.repo.GameRepository;
import com.chessapp.api.domain.repo.MoveRepository;
import com.chessapp.api.domain.repo.PositionRepository;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final ChessComClient chessComClient;
    private final PgnParser pgnParser;
    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final PositionRepository positionRepository;
    private final ResourceLoader resourceLoader;

    public IngestService(ChessComClient chessComClient, PgnParser pgnParser,
                         GameRepository gameRepository, MoveRepository moveRepository,
                         PositionRepository positionRepository, ResourceLoader resourceLoader) {
        this.chessComClient = chessComClient;
        this.pgnParser = pgnParser;
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.positionRepository = positionRepository;
        this.resourceLoader = resourceLoader;
    }

    public void startIngest(String runId, String username, YearMonth from, YearMonth to, boolean offline) {
        log.info("start ingest run {}", runId);
        try {
            if (offline) {
                ingestOffline();
            } else {
                ingestOnline(username, from, to);
            }
        } catch (Exception e) {
            log.error("ingest failed", e);
        }
    }

    private void ingestOffline() throws IOException {
        InputStream is = resourceLoader.getResource("classpath:fixtures/pgn/sample_10_games.pgn").getInputStream();
        String content = new String(is.readAllBytes());
        String[] parts = content.split("\n\n(?=\[Event)");
        for (String pgn : parts) {
            parseAndPersist(pgn.trim());
        }
    }

    private void ingestOnline(String username, YearMonth from, YearMonth to) {
        List<String> archives = chessComClient.listArchives(username).block();
        if (archives == null) return;
        for (String archive : archives) {
            String[] seg = archive.split("/");
            if (seg.length < 2) continue;
            String yyyy = seg[seg.length - 2];
            String mm = seg[seg.length - 1];
            YearMonth ym = YearMonth.of(Integer.parseInt(yyyy), Integer.parseInt(mm));
            if (from != null && ym.isBefore(from)) continue;
            if (to != null && ym.isAfter(to)) continue;
            chessComClient.fetchMonth(username, yyyy, mm).toStream().forEach(node -> {
                JsonNode pgnNode = node.get("pgn");
                if (pgnNode != null) {
                    parseAndPersist(pgnNode.asText());
                }
            });
        }
    }

    private void parseAndPersist(String pgn) {
        PgnParser.ParsedGame parsed = pgnParser.parse(pgn);
        if (parsed.getGameIdExt() != null && gameRepository.findByGameIdExt(parsed.getGameIdExt()).isPresent()) {
            log.debug("game {} already exists", parsed.getGameIdExt());
            return;
        }
        Game g = new Game();
        g.setId(UUID.randomUUID());
        g.setGameIdExt(parsed.getGameIdExt());
        g.setEndTime(parsed.getEndTime());
        g.setTimeControl(parsed.getTimeControl());
        g.setResult(parsed.getResult());
        g.setWhiteRating(parsed.getWhiteRating());
        g.setBlackRating(parsed.getBlackRating());
        g.setPgn(parsed.getPgnRaw());
        gameRepository.save(g);
        List<Move> moves = new ArrayList<>();
        parsed.getMoves().forEach(pm -> {
            Move m = new Move();
            m.setId(UUID.randomUUID());
            m.setGameId(g.getId());
            m.setPly(pm.getPly());
            m.setSan(pm.getSan());
            m.setUci(pm.getUci());
            m.setColor(pm.getColor());
            moves.add(m);
        });
        List<Position> positions = new ArrayList<>();
        parsed.getPositions().forEach(pp -> {
            Position p = new Position();
            p.setId(UUID.randomUUID());
            p.setGameId(g.getId());
            p.setPly(pp.getPly());
            p.setFen(pp.getFen());
            p.setSideToMove(pp.getSideToMove());
            positions.add(p);
        });
        moveRepository.saveAll(moves);
        positionRepository.saveAll(positions);
    }

    // legacy method used by controller
    public void startImport() {
        startIngest(UUID.randomUUID().toString(), "", null, null, true);
    }
}
