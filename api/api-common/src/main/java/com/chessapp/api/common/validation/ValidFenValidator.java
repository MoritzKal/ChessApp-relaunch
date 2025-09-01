package com.chessapp.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidFenValidator implements ConstraintValidator<ValidFen, String> {
    @Override
    public boolean isValid(String fen, ConstraintValidatorContext context) {
        if (fen == null || fen.isBlank()) return true;
        String[] parts = fen.trim().split("\\s+");
        if (parts.length < 4 || parts.length > 6) return false;
        return parts[0].matches("(?i)([prnbqk1-8]{1,8}/){7}[prnbqk1-8]{1,8}")
                && parts[1].matches("[wb]")
                && parts[2].matches("(-|K?Q?k?q?)")
                && parts[3].matches("(-|[a-h][36])");
    }
}
