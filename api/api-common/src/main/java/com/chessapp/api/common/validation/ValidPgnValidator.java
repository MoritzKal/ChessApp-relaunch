package com.chessapp.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidPgnValidator implements ConstraintValidator<ValidPgn, String> {
    private static final Pattern MOVE_PATTERN = Pattern.compile("\\d+\\.\\s*[a-hnbrqkoxO0-9+=#-]+");

    @Override
    public boolean isValid(String pgn, ConstraintValidatorContext context) {
        if (pgn == null || pgn.isBlank()) return true;
        String trimmed = pgn.trim();
        // Strip header tags
        String noHeaders = trimmed.replaceAll("(?m)^\\[.*?\\]\\s*", "").trim();
        if (noHeaders.isEmpty()) return false;
        return MOVE_PATTERN.matcher(noHeaders).find();
    }
}
