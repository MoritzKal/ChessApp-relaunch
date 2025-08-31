package com.chessapp.api.common.validation;

import java.io.StringReader;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.pgn.PGNReader;

public class PGNValidator implements ConstraintValidator<PGN, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            PGNReader reader = new PGNReader(new StringReader(value));
            Game game = reader.parseGame();
            return game != null && game.getHalfMoves().size() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
