package com.slim.kreadevis_backend.dto.professional;

public record ProfessionalResponse(
        Long id,
        String firstName,
        String lastName,
        String company,
        String phone,
        String contactEmail
) {}
