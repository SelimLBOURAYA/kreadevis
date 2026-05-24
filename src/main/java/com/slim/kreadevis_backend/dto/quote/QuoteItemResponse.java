package com.slim.kreadevis_backend.dto.quote;

import com.slim.kreadevis_backend.dto.product.ProductResponse;

import java.math.BigDecimal;

public record QuoteItemResponse(
        Long id,
        ProductResponse product,
        Long quantity,
        BigDecimal unitPrice,
        BigDecimal vatRate,
        BigDecimal totalPrice
) {}
