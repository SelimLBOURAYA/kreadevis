package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.quote.QuoteRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteResponse;
import com.slim.kreadevis_backend.entity.Client;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.QuoteStatus;
import com.slim.kreadevis_backend.mapper.QuoteMapper;
import com.slim.kreadevis_backend.repository.ClientRepository;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.service.QuoteService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyy");

    private final QuoteRepository quoteRepository;
    private final ClientRepository clientRepository;
    private final QuoteMapper quoteMapper;

    @Override
    public List<QuoteResponse> findAll() {
        return quoteRepository.findAll().stream().map(quoteMapper::toResponse).toList();
    }

    @Override
    public QuoteResponse findById(Long id) {
        return quoteMapper.toResponse(quoteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + id)));
    }

    @Override
    public List<QuoteResponse> findByClientId(Long clientId) {
        return quoteRepository.findByClientId(clientId).stream().map(quoteMapper::toResponse).toList();
    }

    @Override
    public QuoteResponse findByReferenceCode(String referenceCode) {
        return quoteMapper.toResponse(quoteRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + referenceCode)));
    }

    @Override
    @Transactional
    public QuoteResponse create(QuoteRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + request.clientId()));
        Quote quote = Quote.builder().client(client).build();
        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Override
    @Transactional
    public QuoteResponse finalize(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + id));

        if (quote.getStatus() == QuoteStatus.FINALIZED) {
            throw new IllegalStateException("Quote already finalized");
        }
        if (quote.getStatus() == QuoteStatus.CANCELLED) {
            throw new IllegalStateException("Cannot finalize a cancelled quote");
        }

        LocalDate today = LocalDate.now();
        int nextSequence = quoteRepository.findMaxDailySequenceByDate(today).orElse(0) + 1;
        quote.setDailySequence(nextSequence);
        quote.setReferenceCode(today.format(DATE_FORMAT) + "-" + String.format("%03d", nextSequence));
        quote.setStatus(QuoteStatus.FINALIZED);

        float total = quote.getItems().stream()
                .reduce(0f, (acc, item) -> acc + item.getTotalPrice(), Float::sum);
        quote.setTotalPrice(total);

        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Override
    @Transactional
    public QuoteResponse pending(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + id));
        if (quote.getStatus() == QuoteStatus.CANCELLED) {
            throw new IllegalStateException("Cannot set a cancelled quote to pending");
        }
        quote.setStatus(QuoteStatus.PENDING);
        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Override
    @Transactional
    public QuoteResponse cancel(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + id));
        if (quote.getStatus() == QuoteStatus.FINALIZED) {
            throw new IllegalStateException("Cannot cancel a finalized quote");
        }
        quote.setStatus(QuoteStatus.CANCELLED);
        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + id));
        quote.getItems().forEach(item -> item.setDeleted(true));
        quote.setDeleted(true);
        quoteRepository.save(quote);
    }
}
