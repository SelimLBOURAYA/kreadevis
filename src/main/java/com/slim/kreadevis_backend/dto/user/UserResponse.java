package com.slim.kreadevis_backend.dto.user;

import java.util.Set;

public record UserResponse(
        Long id,
        String login,
        String email,
        Set<String> roles
) {}
