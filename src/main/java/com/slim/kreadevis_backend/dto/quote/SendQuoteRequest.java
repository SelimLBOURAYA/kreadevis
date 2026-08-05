package com.slim.kreadevis_backend.dto.quote;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Optional body of {@code POST /api/quotes/{id}/send}.
 *
 * @param recipientOverride when set, the quote is sent to this address instead of {@code client.email}
 * @param customMessage     free text inserted in the email body (max 1000 chars, escaped by the template)
 */
public record SendQuoteRequest(
        @Email String recipientOverride,
        @Size(max = 1000) String customMessage
) {}
