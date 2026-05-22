package com.slim.kreadevis_backend.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String label,
        String description,
        @NotNull Long stockQuantity,
        BigDecimal unitPrice,
        BigDecimal vatRate,
        String referenceCode,
        Long supplierId
) {}
