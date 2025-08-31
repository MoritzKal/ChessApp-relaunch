package com.chessapp.api.dataset.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidJsonValidator implements ConstraintValidator<ValidJson, String> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            mapper.readTree(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
