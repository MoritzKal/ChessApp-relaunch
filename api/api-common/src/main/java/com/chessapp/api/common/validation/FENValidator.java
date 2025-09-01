package com.chessapp.api.common.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.github.bhlangonijr.chesslib.Board;

public class FENValidator implements ConstraintValidator<FEN, String> {

    private static final Pattern FEN_PATTERN = Pattern.compile(
            "^([rnbqkpRNBQKP1-8]+/){7}[rnbqkpRNBQKP1-8]+\\s[bw]\\s(-|[KQkq]{1,4})\\s(-|[a-h][36])\\s\\d+\\s\\d+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (!FEN_PATTERN.matcher(value).matches()) {
            return false;
        }
        try {
            Board board = new Board();
            board.loadFromFen(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
