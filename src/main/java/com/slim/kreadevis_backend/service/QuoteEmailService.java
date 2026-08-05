package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.quote.SendQuoteRequest;
import com.slim.kreadevis_backend.dto.quote.SendQuoteResponse;

public interface QuoteEmailService {

    /**
     * Renders the quote PDF and emails it to the client.
     *
     * @param quoteId the quote to send
     * @param request optional recipient override and custom message (may be {@code null})
     * @return the send timestamp and the address actually used
     */
    SendQuoteResponse sendQuoteToClient(Long quoteId, SendQuoteRequest request);
}
