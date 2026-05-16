package com.slim.kreadevis_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, long accessTokenExpirationMs, long refreshTokenExpirationMs) {}
