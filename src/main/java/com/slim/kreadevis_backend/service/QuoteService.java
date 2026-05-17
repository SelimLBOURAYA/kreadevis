package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.quote.QuoteRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteResponse;

import java.util.List;

public interface QuoteService {
    List<QuoteResponse> findAll();
    QuoteResponse findById(Long id);
    List<QuoteResponse> findByClientId(Long clientId);
    QuoteResponse findByReferenceCode(String referenceCode);
    QuoteResponse create(QuoteRequest request);
    QuoteResponse finalize(Long id);
    QuoteResponse pending(Long id);
    QuoteResponse cancel(Long id);
    void delete(Long id);
}
