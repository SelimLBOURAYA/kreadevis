package com.slim.kreadevis_backend.dto.quote;

import jakarta.validation.constraints.NotNull;

public record QuoteRequest(
        @NotNull Long clientId
) {}
