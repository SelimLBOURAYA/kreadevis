package com.slim.kreadevis_backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        @JsonProperty("tokenType") String tokenType
) {
    public AuthResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }

    public AuthResponse(String accessToken) {
        this(accessToken, null, "Bearer");
    }
}
