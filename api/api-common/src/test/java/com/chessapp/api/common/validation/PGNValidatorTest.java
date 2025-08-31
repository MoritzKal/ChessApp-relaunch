package com.chessapp.api.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PGNValidatorTest {

    private Validator validator;

    record PgnHolder(@PGN String pgn) {}

    @BeforeEach
    void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
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
