package com.slim.kreadevis_backend.dto.product;

public record ProductResponse(
        Long id,
        String label,
        String description,
        Long stockQuantity,
        float unitPrice,
        float vatRate,
        String referenceCode,
        Long supplierId
) {}
