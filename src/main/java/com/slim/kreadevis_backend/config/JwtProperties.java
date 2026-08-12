package com.slim.kreadevis_backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.jwt")
@Validated
public record JwtProperties(
        @NotBlank @Size(min = 64) String secret,
        @Positive long accessTokenExpirationMs,
        @Positive long refreshTokenExpirationMs
) {}
