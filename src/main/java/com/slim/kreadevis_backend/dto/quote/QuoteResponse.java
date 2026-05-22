package com.slim.kreadevis_backend.dto.quote;

import com.slim.kreadevis_backend.dto.client.ClientResponse;
import com.slim.kreadevis_backend.entity.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record QuoteResponse(
        Long id,
        String referenceCode,
        LocalDate date,
        BigDecimal totalPrice,
        QuoteStatus status,
        ClientResponse client,
        List<QuoteItemResponse> items
) {}
