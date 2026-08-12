package com.slim.kreadevis_backend.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fail-fast behavior (security.md §1) relies on {@code @NotBlank}/{@code @Size}
 * constraints on {@link JwtProperties} being enforced when Spring binds
 * {@code app.jwt.*} at startup. This exercises those constraints directly — a full
 * "app refuses to boot without JWT_SECRET" check would require a dedicated
 * {@code SpringApplication} run, out of scope for a unit test.
 */
class JwtPropertiesTest {

    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    @Test
    void rejectsBlankSecret() {
        JwtProperties properties = new JwtProperties("", 3_600_000L, 86_400_000L);

        Set<ConstraintViolation<JwtProperties>> violations = VALIDATOR.validate(properties);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void rejectsSecretShorterThan64Characters() {
        JwtProperties properties = new JwtProperties("too-short", 3_600_000L, 86_400_000L);

        Set<ConstraintViolation<JwtProperties>> violations = VALIDATOR.validate(properties);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void rejectsNonPositiveExpirations() {
        String secret = "a".repeat(64);
        JwtProperties properties = new JwtProperties(secret, 0L, -1L);

        Set<ConstraintViolation<JwtProperties>> violations = VALIDATOR.validate(properties);

        assertThat(violations).hasSize(2);
    }

    @Test
    void acceptsValidProperties() {
        String secret = "a".repeat(64);
        JwtProperties properties = new JwtProperties(secret, 3_600_000L, 86_400_000L);

        Set<ConstraintViolation<JwtProperties>> violations = VALIDATOR.validate(properties);

        assertThat(violations).isEmpty();
    }
}
