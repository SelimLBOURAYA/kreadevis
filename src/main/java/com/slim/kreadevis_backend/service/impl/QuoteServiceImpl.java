package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.quote.QuoteRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteResponse;
import com.slim.kreadevis_backend.entity.Client;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.QuoteItem;
import com.slim.kreadevis_backend.entity.QuoteStatus;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.mapper.QuoteMapper;
import com.slim.kreadevis_backend.repository.ClientRepository;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.repository.UserRepository;
import com.slim.kreadevis_backend.security.UserDetailsImpl;
import com.slim.kreadevis_backend.service.QuoteService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyy");

    private final QuoteRepository quoteRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final QuoteMapper quoteMapper;

    @Override
    public List<QuoteResponse> findAll(LocalDate startDate, LocalDate endDate) {
        return quoteRepository.findByDateRange(startDate, endDate).stream().map(quoteMapper::toResponse).toList();
    }

    @Override
    public QuoteResponse findById(Long id) {
        return quoteMapper.toResponse(quoteRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + id)));
    }

    @Override
    public List<QuoteResponse> findByClientId(Long clientId) {
        return quoteRepository.findByClientIdAndActiveTrue(clientId).stream().map(quoteMapper::toResponse).toList();
    }

    @Override
    public QuoteResponse findByReferenceCode(String referenceCode) {
        return quoteMapper.toResponse(quoteRepository.findByReferenceCodeAndActiveTrue(referenceCode)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + referenceCode)));
    }

    @Override
    @Transactional
    public QuoteResponse create(QuoteRequest request) {
        Client client = clientRepository.findByIdAndActiveTrue(request.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + request.clientId()));
        Quote quote = Quote.builder()
                .client(client)
                .createdBy(getCurrentUser())
                .build();
        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Override
    @Transactional
    public QuoteResponse finalize(Long id) {
        Quote quote = quoteRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + id));

        if (quote.getStatus() == QuoteStatus.FINALIZED) {
            throw new IllegalStateException("Quote already finalized");
        }
        if (quote.getStatus() == QuoteStatus.CANCELLED) {
            throw new IllegalStateException("Cannot finalize a cancelled quote");
        }

        assignReferenceCode(quote);
        quote.setStatus(QuoteStatus.FINALIZED);
        quote.setTotalPrice(computeTotalPrice(quote));

        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Override
    @Transactional
    public QuoteResponse pending(Long id) {
        Quote quote = quoteRepository.findByIdAndActiveTrue(id)
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
        Quote quote = quoteRepository.findByIdAndActiveTrue(id)
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
        quote.getItems().forEach(item -> item.setActive(false));
        quote.setActive(false);
        quoteRepository.save(quote);
    }

    private void assignReferenceCode(Quote quote) {
        User currentUser = getCurrentUser();
        LocalDate today = LocalDate.now();
        int nextSequence = quoteRepository.findMaxDailySequenceByUserAndDate(currentUser.getId(), today) + 1;
        quote.setDailySequence(nextSequence);
        quote.setReferenceCode(today.format(DATE_FORMAT) + "-" + currentUser.getId() + "-" + String.format("%03d", nextSequence));
    }

    private BigDecimal computeTotalPrice(Quote quote) {
        return quote.getItems().stream()
                .map(QuoteItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var userDetails = (UserDetailsImpl) auth.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB"));
    }
}
