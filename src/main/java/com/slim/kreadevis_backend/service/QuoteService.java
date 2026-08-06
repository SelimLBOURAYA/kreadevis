package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.quote.QuoteRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteResponse;
import com.slim.kreadevis_backend.entity.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface QuoteService {
    Page<QuoteResponse> findAll(QuoteStatus status, LocalDate startDate, LocalDate endDate, Pageable pageable);
    QuoteResponse findById(Long id);
    List<QuoteResponse> findByClientId(Long clientId);
    QuoteResponse findByReferenceCode(String referenceCode);
    QuoteResponse create(QuoteRequest request);
    QuoteResponse finalize(Long id);
    QuoteResponse pending(Long id);
    QuoteResponse cancel(Long id);
    void delete(Long id);
}
