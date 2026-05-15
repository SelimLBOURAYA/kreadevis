package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.service.QuoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class QuoteServiceImpl implements QuoteService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyy");

    private final QuoteRepository quoteRepository;

    public QuoteServiceImpl(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    @Override
    public String generateReferenceCode(Quote quote) {
        if (quote.getReferenceCode() != null) {
            return quote.getReferenceCode();
        }

        LocalDate today = LocalDate.now();
        int maxSequence = quoteRepository.findMaxDailySequenceByDate(today).orElse(0);
        int nextSequence = maxSequence + 1;

        quote.setDailySequence(nextSequence);
        String referenceCode = today.format(DATE_FORMAT) + "-" + String.format("%03d", nextSequence);
        quote.setReferenceCode(referenceCode);

        return referenceCode;
    }
}
