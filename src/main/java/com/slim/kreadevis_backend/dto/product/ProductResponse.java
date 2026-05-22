package com.slim.kreadevis_backend.dto.product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String label,
        String description,
        Long stockQuantity,
        BigDecimal unitPrice,
        BigDecimal vatRate,
        String referenceCode,
        Long supplierId
) {}
