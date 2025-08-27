package com.chessapp.api.ingest.service;

import com.chessapp.api.domain.entity.Color;
import com.chessapp.api.domain.entity.GameResult;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.SanUtils;
import com.github.bhlangonijr.chesslib.pgn.PgnHolder;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PgnParser {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ParsedGame parse(String pgnRaw) {
        try {
            PgnHolder holder = new PgnHolder();
            holder.loadPgnFromString(pgnRaw);
            Game game = holder.getGames().get(0);
            Map<String,String> tags = game.getTags();

            String site = tags.getOrDefault("Site", "");
            String link = tags.getOrDefault("Link", site);
            String gameIdExt = extractGameId(link);
            if (gameIdExt == null || gameIdExt.isEmpty()) {
                gameIdExt = Integer.toHexString(pgnRaw.hashCode());
            }

            String utcDate = tags.get("UTCDate");
            String utcTime = tags.get("UTCTime");
            Instant endTime = null;
            if (utcDate != null && utcTime != null) {
                LocalDate d = LocalDate.parse(utcDate, DATE_FMT);
                LocalTime t = LocalTime.parse(utcTime, TIME_FMT);
                endTime = ZonedDateTime.of(d, t, ZoneOffset.UTC).toInstant();
            }

            String timeControl = tags.get("TimeControl");
            String resultTag = tags.get("Result");
            GameResult result = switch (resultTag) {
                case "1-0" -> GameResult.WHITE_WIN;
                case "0-1" -> GameResult.BLACK_WIN;
                case "1/2-1/2" -> GameResult.DRAW;
                default -> GameResult.ABORTED;
            };
            Integer whiteRating = parseInt(tags.get("WhiteElo"));
            Integer blackRating = parseInt(tags.get("BlackElo"));

            Board board = new Board();
            List<ParsedMove> moves = new ArrayList<>();
            List<ParsedPosition> positions = new ArrayList<>();
            int ply = 1;
            for (Move mv : game.getHalfMoves()) {
                String san = SanUtils.getSan(board, mv);
                board.doMove(mv);
                String uci = mv.toString();
                Color color = ply % 2 == 1 ? Color.WHITE : Color.BLACK;
                moves.add(new ParsedMove(ply, san, uci, color));
                List<String> legal = board.legalMoves().stream().map(Move::toString).collect(Collectors.toList());
                positions.add(new ParsedPosition(ply, board.getFen(), toColor(board.getSideToMove()), legal));
                ply++;
            }

            return new ParsedGame(gameIdExt, endTime, timeControl, result, whiteRating, blackRating, pgnRaw, moves, positions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PGN", e);
        }
    }

    private static Integer parseInt(String s) {
        try { return s != null ? Integer.valueOf(s) : null; }
        catch (NumberFormatException e) { return null; }
    }

    private static String extractGameId(String url) {
        int idx = url == null ? -1 : url.lastIndexOf('/');
        return idx >= 0 ? url.substring(idx + 1) : null;
    }

    private static Color toColor(Side side) {
        return side == Side.WHITE ? Color.WHITE : Color.BLACK;
    }

    public record ParsedMove(int ply, String san, String uci, Color color) {}
    public record ParsedPosition(int ply, String fen, Color sideToMove, List<String> legalMoves) {}
    public record ParsedGame(String gameIdExt, Instant endTime, String timeControl, GameResult result,
                             Integer whiteRating, Integer blackRating, String pgnRaw,
                             List<ParsedMove> moves, List<ParsedPosition> positions) {}
}
