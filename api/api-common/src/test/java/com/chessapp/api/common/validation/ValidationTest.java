package com.chessapp.api.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidationTest {

    private Validator validator;

    record FenHolder(@FEN String fen) {}
    record PgnHolder(@PGN String pgn) {}

    @BeforeEach
    void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validFen_passesValidation() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        var violations = validator.validate(new FenHolder(fen));
        assertThat(violations).isEmpty();
    }

    @Test
    void invalidFen_failsValidation() {
        var violations = validator.validate(new FenHolder("invalid"));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void validPgn_passesValidation() {
        String pgn = "[Event \"?\"]\n\n1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 1/2-1/2";
        var violations = validator.validate(new PgnHolder(pgn));
        assertThat(violations).isEmpty();
    }

    @Test
    void invalidPgn_failsValidation() {
        var violations = validator.validate(new PgnHolder("invalid"));
        assertThat(violations).isNotEmpty();
    }
}
