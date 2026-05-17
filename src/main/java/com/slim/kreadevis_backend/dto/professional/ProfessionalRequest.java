package com.slim.kreadevis_backend.dto.professional;

import jakarta.validation.constraints.NotBlank;

public record ProfessionalRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        String contactEmail,
        @NotBlank String streetNumber,
        @NotBlank String street,
        @NotBlank String zipCode,
        @NotBlank String city,
        String company,
        String vat,
        String siren
) {}
