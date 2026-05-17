package com.slim.kreadevis_backend.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientRequest(
        String firstName,
        @NotBlank String lastName,
        String company,
        String siret,
        String siren,
        String vatCode,
        String phone,
        @Email String email,
        @NotNull Long addressId
) {}
