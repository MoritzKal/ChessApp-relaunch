package com.chessapp.api.ingest.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.chessapp.api.domain.entity.Color;
import com.chessapp.api.domain.entity.GameResult;

@Component
public class PgnParser {

    private static final Pattern TAG_PATTERN = Pattern.compile("\\[(\\w+) \"([^\"]*)\"\\]");

    public ParsedGame parse(String pgn) {
        Map<String, String> tags = new HashMap<>();
        Matcher m = TAG_PATTERN.matcher(pgn);
        int last = 0;
        while (m.find()) {
            tags.put(m.group(1), m.group(2));
            last = m.end();
        }
        String movesSection = pgn.substring(last).trim();
        ParsedGame game = new ParsedGame();
        game.setGameIdExt(tags.get("UUID"));
        game.setTimeControl(tags.get("TimeControl"));
        String date = tags.get("Date");
        if (date != null && date.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) {
            LocalDate ld = LocalDate.parse(date.replace('.', '-'));
            game.setEndTime(ld.atStartOfDay().toInstant(ZoneOffset.UTC));
        }
        String res = tags.get("Result");
        if ("1-0".equals(res)) game.setResult(GameResult.WHITE_WIN);
        else if ("0-1".equals(res)) game.setResult(GameResult.BLACK_WIN);
        else if ("1/2-1/2".equals(res)) game.setResult(GameResult.DRAW);
        game.setWhiteRating(parseInt(tags.get("WhiteElo")));
        game.setBlackRating(parseInt(tags.get("BlackElo")));
        game.setPgnRaw(pgn);
        List<ParsedMove> moves = new ArrayList<>();
        List<ParsedPosition> positions = new ArrayList<>();
        String[] tokens = movesSection.split("\\s+");
        int ply = 1;
        Color turn = Color.WHITE;
        for (String tok : tokens) {
            if (tok.isEmpty() || tok.matches(".*\\d+\\.") || tok.matches("\\d+\.\\d+")) {
                continue;
            }
            if (tok.contains(".")) {
                continue; // skip move numbers like 1.
            }
            if (tok.equals("1-0") || tok.equals("0-1") || tok.equals("1/2-1/2") || tok.equals("*")) {
                break;
            }
            ParsedMove mv = new ParsedMove();
            mv.setPly(ply);
            mv.setSan(tok);
            mv.setColor(turn);
            moves.add(mv);
            ParsedPosition pos = new ParsedPosition();
            pos.setPly(ply);
            pos.setFen("");
            pos.setSideToMove(turn == Color.WHITE ? Color.BLACK : Color.WHITE);
            positions.add(pos);
            ply++;
            turn = turn == Color.WHITE ? Color.BLACK : Color.WHITE;
        }
        game.setMoves(moves);
        game.setPositions(positions);
        return game;
    }

    private Integer parseInt(String s) {
        try {
            return s == null ? null : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static class ParsedGame {
        private String gameIdExt;
        private Instant endTime;
        private String timeControl;
        private GameResult result;
        private Integer whiteRating;
        private Integer blackRating;
        private String pgnRaw;
        private List<ParsedMove> moves;
        private List<ParsedPosition> positions;
        public String getGameIdExt() { return gameIdExt; }
        public void setGameIdExt(String gameIdExt) { this.gameIdExt = gameIdExt; }
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        public String getTimeControl() { return timeControl; }
        public void setTimeControl(String timeControl) { this.timeControl = timeControl; }
        public GameResult getResult() { return result; }
        public void setResult(GameResult result) { this.result = result; }
        public Integer getWhiteRating() { return whiteRating; }
        public void setWhiteRating(Integer whiteRating) { this.whiteRating = whiteRating; }
        public Integer getBlackRating() { return blackRating; }
        public void setBlackRating(Integer blackRating) { this.blackRating = blackRating; }
        public String getPgnRaw() { return pgnRaw; }
        public void setPgnRaw(String pgnRaw) { this.pgnRaw = pgnRaw; }
        public List<ParsedMove> getMoves() { return moves; }
        public void setMoves(List<ParsedMove> moves) { this.moves = moves; }
        public List<ParsedPosition> getPositions() { return positions; }
        public void setPositions(List<ParsedPosition> positions) { this.positions = positions; }
    }

    public static class ParsedMove {
        private int ply;
        private String san;
        private String uci;
        private Color color;
        public int getPly() { return ply; }
        public void setPly(int ply) { this.ply = ply; }
        public String getSan() { return san; }
        public void setSan(String san) { this.san = san; }
        public String getUci() { return uci; }
        public void setUci(String uci) { this.uci = uci; }
        public Color getColor() { return color; }
        public void setColor(Color color) { this.color = color; }
    }

    public static class ParsedPosition {
        private int ply;
        private String fen;
        private Color sideToMove;
        public int getPly() { return ply; }
        public void setPly(int ply) { this.ply = ply; }
        public String getFen() { return fen; }
        public void setFen(String fen) { this.fen = fen; }
        public Color getSideToMove() { return sideToMove; }
        public void setSideToMove(Color sideToMove) { this.sideToMove = sideToMove; }
    }
}
