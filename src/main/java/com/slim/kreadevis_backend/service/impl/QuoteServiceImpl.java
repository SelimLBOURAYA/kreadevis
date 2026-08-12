package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.quote.QuoteRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteResponse;
import com.slim.kreadevis_backend.entity.Client;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.QuoteStatus;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.mapper.QuoteMapper;
import com.slim.kreadevis_backend.repository.ClientRepository;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import com.slim.kreadevis_backend.service.QuoteService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public Page<QuoteResponse> findAll(QuoteStatus status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Quote> quotes = securityUtils.resolveOwned(
                () -> quoteRepository.findByFilters(status, startDate, endDate, pageable),
                ownerId -> quoteRepository.findByFiltersForOwner(ownerId, status, startDate, endDate, pageable));
        return quotes.map(quoteMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteResponse findById(Long id) {
        return quoteMapper.toResponse(getOwnedQuote(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteResponse> findByClientId(Long clientId) {
        List<Quote> quotes = securityUtils.resolveOwned(
                () -> quoteRepository.findByClientIdAndActiveTrue(clientId),
                ownerId -> quoteRepository.findByClientIdAndActiveTrueAndCreatedById(clientId, ownerId));
        return quotes.stream().map(quoteMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteResponse findByReferenceCode(String referenceCode) {
        var quote = securityUtils.resolveOwned(
                () -> quoteRepository.findByReferenceCodeAndActiveTrue(referenceCode),
                ownerId -> quoteRepository.findByReferenceCodeAndActiveTrueAndCreatedById(referenceCode, ownerId));
        return quoteMapper.toResponse(quote.orElseThrow(() -> new EntityNotFoundException("Quote not found: " + referenceCode)));
    }

    @Override
    @Transactional
    public QuoteResponse create(QuoteRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        var foundClient = securityUtils.resolveOwned(
                () -> clientRepository.findByIdAndActiveTrue(request.clientId()),
                ownerId -> clientRepository.findByIdAndActiveTrueAndCreatedById(request.clientId(), ownerId));
        Client client = foundClient
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + request.clientId()));
        Quote quote = Quote.builder()
                .client(client)
                .createdBy(currentUser)
                .build();
        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Override
    @Transactional
    public QuoteResponse finalize(Long id) {
        Quote quote = getOwnedQuote(id);

        if (quote.getStatus() == QuoteStatus.FINALIZED) {
            throw new IllegalStateException("Quote already finalized");
        }
        if (quote.getStatus() == QuoteStatus.CANCELLED) {
            throw new IllegalStateException("Cannot finalize a cancelled quote");
        }

        quote.setDate(LocalDate.now());
        assignReferenceCode(quote);
        quote.setStatus(QuoteStatus.FINALIZED);
        quote.recomputeTotals();

        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Override
    @Transactional
    public QuoteResponse pending(Long id) {
        Quote quote = getOwnedQuote(id);
        if (quote.getStatus() == QuoteStatus.CANCELLED) {
            throw new IllegalStateException("Cannot set a cancelled quote to pending");
        }
        quote.setStatus(QuoteStatus.PENDING);
        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Override
    @Transactional
    public QuoteResponse cancel(Long id) {
        Quote quote = getOwnedQuote(id);
        if (quote.getStatus() == QuoteStatus.FINALIZED) {
            throw new IllegalStateException("Cannot cancel a finalized quote");
        }
        quote.setStatus(QuoteStatus.CANCELLED);
        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Quote quote = securityUtils.resolveOwned(
                        () -> quoteRepository.findById(id),
                        ownerId -> quoteRepository.findByIdAndCreatedById(id, ownerId))
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + id));
        if (quote.getStatus() == QuoteStatus.FINALIZED) {
            throw new IllegalStateException("Cannot delete a finalized quote");
        }
        quote.getItems().forEach(item -> item.setActive(false));
        quote.setActive(false);
        quoteRepository.save(quote);
    }

    /** Owner-scoped lookup: ROLE_ADMIN bypasses the ownership filter, everyone else only sees their own quotes. */
    private Quote getOwnedQuote(Long id) {
        return securityUtils.resolveOwned(
                        () -> quoteRepository.findByIdAndActiveTrue(id),
                        ownerId -> quoteRepository.findByIdAndActiveTrueAndCreatedById(id, ownerId))
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + id));
    }

    private void assignReferenceCode(Quote quote) {
        User currentUser = securityUtils.getCurrentUser();
        LocalDate today = LocalDate.now();
        int nextSequence = quoteRepository.findMaxDailySequenceByUserAndDate(currentUser.getId(), today) + 1;
        quote.setDailySequence(nextSequence);
        quote.setReferenceCode(today.format(DATE_FORMAT) + "-" + currentUser.getId() + "-" + String.format("%03d", nextSequence));
    }
}
