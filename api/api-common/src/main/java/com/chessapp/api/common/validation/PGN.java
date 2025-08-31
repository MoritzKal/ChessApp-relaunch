package com.chessapp.api.common.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = PGNValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface PGN {
    String message() default "Invalid PGN";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
