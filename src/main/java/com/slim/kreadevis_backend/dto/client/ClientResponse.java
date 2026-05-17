package com.slim.kreadevis_backend.dto.client;

import com.slim.kreadevis_backend.dto.address.AddressResponse;

public record ClientResponse(
        Long id,
        String firstName,
        String lastName,
        String company,
        String email,
        String phone,
        AddressResponse address
) {}
