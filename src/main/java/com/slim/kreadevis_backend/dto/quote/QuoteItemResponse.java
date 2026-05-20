package com.slim.kreadevis_backend.dto.quote;

import com.slim.kreadevis_backend.dto.product.ProductResponse;

public record QuoteItemResponse(
        Long id,
        ProductResponse product,
        Long quantity,
        float unitPrice,
        float totalPrice
) {}
