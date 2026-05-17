package com.slim.kreadevis_backend.dto.address;

public record AddressResponse(
        Long id,
        String streetNumber,
        String street,
        String postalCode,
        String city
) {}
