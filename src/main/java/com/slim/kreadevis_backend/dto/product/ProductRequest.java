package com.slim.kreadevis_backend.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String label,
        String description,
        @NotNull @PositiveOrZero Long stockQuantity,
        @PositiveOrZero BigDecimal unitPrice,
        @PositiveOrZero BigDecimal vatRate,
        String referenceCode,
        Long supplierId
) {}
