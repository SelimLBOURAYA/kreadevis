package com.slim.kreadevis_backend.dto.quote;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuoteItemRequest(
        @NotNull Long productId,
        @NotNull @Positive Long quantity
) {}
