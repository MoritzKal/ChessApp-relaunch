package com.chessapp.api.ingest.service;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import com.github.bhlangonijr.chesslib.pgn.PgnHolder;
import lombok.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PGN -> ParsedGame(s)
 * - Kompatibel mit chesslib 1.3.4
 * - Kein SanUtils, kein loadPgnFromString, keine Game#getTags()
 * - Header werden direkt aus dem PGN-Text geparst
 */
public class PgnParser {

    // ===== Public API ========================================================

    /** ParsedGame als record für einfache DTO-Verwendung */
    public record ParsedGame(
            String gameIdExt,
            Instant endTime,
            String timeControl,
            String result,
            Integer whiteRating,
            Integer blackRating,
            String pgnRaw,
            List<ParsedMove> moves,
            List<ParsedPosition> positions
    ) {}

    public record ParsedMove(int ply, String uci, String color) {}
    public record ParsedPosition(int ply, String fen, String sideToMove) {}

    /**
     * Parst einen einzelnen PGN-String in ein ParsedGame.
     */
    public Optional<ParsedGame> parseOneFromString(@NonNull String pgn) {
        List<ParsedGame> all = parseManyFromConcatPgn(pgn);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    /**
     * Parst mehrere hintereinander stehende PGNs (konkatenierter Text).
     */
    public List<ParsedGame> parseManyFromConcatPgn(@NonNull String allPgn) {
        // 1) In einzelne Spiele (Header+Movetext) splitten
        List<String> chunks = splitIntoGames(allPgn);
        if (chunks.isEmpty()) return List.of();

        List<ParsedGame> out = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            // 2) Mit chesslib aus Datei laden (1.3.4 kann nicht direkt aus String)
            Optional<Game> maybeGame = loadSingleGameViaTempfile(chunk);
            if (maybeGame.isEmpty()) continue;

            Game g = maybeGame.get();

            // 3) Moves holen (chesslib)
            MoveList moves = g.getHalfMoves(); // bei PgnHolder.loadPgn() sind die Moves bereits geparst

            // 4) Header aus dem PGN-Chunk selbst parsen
            Map<String, String> tags = parseHeaders(chunk);

            String timeControl = tags.getOrDefault("TimeControl", "");
            String result = tags.getOrDefault("Result", "");
            Integer wElo = parseInt(tags.get("WhiteElo"));
            Integer bElo = parseInt(tags.get("BlackElo"));
            Instant endTime = parseUtc(tags.get("UTCDate"), tags.get("UTCTime"));
            String gameIdExt = bestEffortGameId(tags, chunk);

            // 5) FEN & UCI je Ply erzeugen durch Re-Play auf dem Board
            Board board = new Board(); // Startstellung
            List<ParsedMove> parsedMoves = new ArrayList<>(moves.size());
            List<ParsedPosition> positions = new ArrayList<>(moves.size());

            int ply = 0;
            for (Move m : moves) {
                ply++;
                // Color ist die Seite, die VOR dem Zug am Zug war
                Side mover = board.getSideToMove();
                parsedMoves.add(new ParsedMove(ply, toUci(m), mover.name()));

                board.doMove(m); // Zug ausführen

                positions.add(new ParsedPosition(
                        ply,
                        board.getFen(),
                        board.getSideToMove().name()
                ));
            }

            out.add(new ParsedGame(
                    gameIdExt,
                    endTime,
                    timeControl,
                    result,
                    wElo,
                    bElo,
                    chunk,
                    Collections.unmodifiableList(parsedMoves),
                    Collections.unmodifiableList(positions)
            ));
        }
        return out;
    }

    // ===== Helpers ===========================================================

    private static Optional<Game> loadSingleGameViaTempfile(String pgnText) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("ingest-", ".pgn");
            Files.writeString(tmp, pgnText, StandardCharsets.UTF_8);

            PgnHolder holder = new PgnHolder(tmp.toString());
            holder.loadPgn(); // 1.3.4: nur Datei-API vorhanden

            List<Game> games = holder.getGames();
            if (games == null || games.isEmpty()) return Optional.empty();

            // Wir erwarten genau 1 Game pro Chunk
            return Optional.of(games.get(0));
        } catch (Exception e) {
            return Optional.empty();
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
        }
    }

    /** PGN-Chunks anhand des Event-Headers trennen. */
    private static List<String> splitIntoGames(String allPgn) {
        // „[Event "…"]“ ist in jedem gültigen PGN-Header vorhanden.
        Pattern pat = Pattern.compile(
                "(?ms)^\\s*\\[Event\\s+\".*?\"\\].*?(?=^\\s*\\[Event\\s+\"|\\z)"
        );
        Matcher m = pat.matcher(allPgn);
        List<String> chunks = new ArrayList<>();
        while (m.find()) {
            String chunk = m.group().trim();
            if (!chunk.isEmpty()) chunks.add(chunk);
        }
        // Fallback: wenn kein Event-Header gefunden wurde, ganzen Text als ein Spiel behandeln
        if (chunks.isEmpty() && !allPgn.isBlank()) {
            chunks.add(allPgn.trim());
        }
        return chunks;
    }

    /** Header-Zeilen im Format [Tag "Value"] oben im PGN-Block parsen. */
    private static Map<String, String> parseHeaders(String pgn) {
        Map<String, String> tags = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new StringReader(pgn))) {
            String line;
            Pattern tag = Pattern.compile("^\\[(\\w+)\\s+\"(.*)\"\\]$");
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) break; // nach Header folgen Leerzeile + Movetext
                Matcher m = tag.matcher(t);
                if (m.matches()) {
                    tags.put(m.group(1), m.group(2));
                } else if (!t.startsWith("[")) {
                    // Falls Movetext ohne Leerzeile beginnt → Header Ende
                    break;
                }
            }
        } catch (IOException ignored) {}
        return tags;
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Instant parseUtc(String utcDate, String utcTime) {
        // chess.com: UTCDate "YYYY.MM.DD", UTCTime "HH:mm:ss"
        try {
            if (utcDate == null || utcTime == null) return null;
            String d = utcDate.replace('.', '-'); // "YYYY-MM-DD"
            LocalDate date = LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime time = LocalTime.parse(utcTime, DateTimeFormatter.ofPattern("HH:mm:ss"));
            return date.atTime(time).toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return null;
        }
    }

    private static String bestEffortGameId(Map<String, String> tags, String pgnRaw) {
        String link = tags.getOrDefault("Link", "");
        String site = tags.getOrDefault("Site", "");
        String id = lastPathSegment(link);
        if (id == null || id.isBlank()) id = lastPathSegment(site);
        if (id == null || id.isBlank()) id = sha1(pgnRaw);
        return id;
    }

    private static String lastPathSegment(String url) {
        if (url == null) return null;
        int q = url.indexOf('?');
        String base = q >= 0 ? url.substring(0, q) : url;
        int i = base.lastIndexOf('/');
        return i >= 0 && i < base.length() - 1 ? base.substring(i + 1) : base;
    }

    private static String toUci(Move m) {
        // UCI: e2e4[, promotion]
        Square from = m.getFrom();
        Square to = m.getTo();
        String uci = from.value().toLowerCase() + to.value().toLowerCase();
        if (m.getPromotion() != null) {
            char promo = switch (m.getPromotion().getPieceType()) {
                case QUEEN -> 'q';
                case ROOK -> 'r';
                case BISHOP -> 'b';
                case KNIGHT -> 'n';
                default -> 0;
            };
            if (promo != 0) uci += promo;
        }
        return uci;
    }

    private static String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
