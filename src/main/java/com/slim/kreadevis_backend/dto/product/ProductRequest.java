package com.slim.kreadevis_backend.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotBlank String label,
        String description,
        @NotNull Long stockQuantity,
        float unitPrice,
        float vatRate,
        String referenceCode,
        Long supplierId
) {}
