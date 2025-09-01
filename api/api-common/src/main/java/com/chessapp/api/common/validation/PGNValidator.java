package com.chessapp.api.common.validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.pgn.PgnHolder;

public class PGNValidator implements ConstraintValidator<PGN, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        Path tmp = null;
        try {
            // chesslib 1.3.x supports loading PGN only from file via PgnHolder
            tmp = Files.createTempFile("pgn-validate-", ".pgn");
            Files.writeString(tmp, value, StandardCharsets.UTF_8);

            PgnHolder holder = new PgnHolder(tmp.toString());
            holder.loadPgn();

            if (holder.getGames() == null || holder.getGames().isEmpty()) {
                return false;
            }
            // Validate that at least one game has parsed moves
            for (Game g : holder.getGames()) {
                if (g != null && g.getHalfMoves() != null && !g.getHalfMoves().isEmpty()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
        }
    }
}
