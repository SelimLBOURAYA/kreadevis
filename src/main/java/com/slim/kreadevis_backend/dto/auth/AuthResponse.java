package com.slim.kreadevis_backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
        String accessToken,
        @JsonProperty("tokenType") String tokenType
) {
    public AuthResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
