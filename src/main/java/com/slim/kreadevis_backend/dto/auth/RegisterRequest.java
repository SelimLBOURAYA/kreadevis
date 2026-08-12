package com.slim.kreadevis_backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String login,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, max = 100) String password
) {}
