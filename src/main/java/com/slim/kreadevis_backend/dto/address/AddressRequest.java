package com.slim.kreadevis_backend.dto.address;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        String streetNumber,
        @NotBlank String street,
        @NotBlank String postalCode,
        @NotBlank String city
) {}
