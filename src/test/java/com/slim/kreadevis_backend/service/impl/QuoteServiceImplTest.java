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
import com.slim.kreadevis_backend.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteServiceImplTest {

    @Mock private QuoteRepository quoteRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private QuoteMapper quoteMapper;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private QuoteServiceImpl quoteService;

    private static final Long OWNER_ID = 42L;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(OWNER_ID);
        currentUser.setLogin("testuser");
    }

    // --- findAll ---

    @Test
    void findAll_shouldReturnMappedPage_scopedToOwner_whenNotAdmin() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        Quote quote = new Quote();
        QuoteResponse response = dummyResponse();
        Pageable pageable = PageRequest.of(0, 20);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByFiltersForOwner(OWNER_ID, QuoteStatus.DRAFT, start, end, pageable))
                .thenReturn(new PageImpl<>(List.of(quote)));
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        Page<QuoteResponse> result = quoteService.findAll(QuoteStatus.DRAFT, start, end, pageable);

        assertThat(result.getContent()).hasSize(1).contains(response);
    }

    @Test
    void findAll_shouldReturnAllQuotes_whenAdmin() {
        Quote quote = new Quote();
        QuoteResponse response = dummyResponse();
        Pageable pageable = PageRequest.of(0, 20);
        when(securityUtils.isAdmin()).thenReturn(true);
        when(quoteRepository.findByFilters(null, null, null, pageable)).thenReturn(new PageImpl<>(List.of(quote)));
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        Page<QuoteResponse> result = quoteService.findAll(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1).contains(response);
        verify(quoteRepository, never()).findByFiltersForOwner(any(), any(), any(), any(), any());
    }

    // --- findById ---

    @Test
    void findById_shouldReturnResponse_whenOwnedByCurrentUser() {
        Quote quote = new Quote();
        QuoteResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        QuoteResponse result = quoteService.findById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_shouldThrow404_whenOwnedByAnotherUser() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Quote not found: 99");
    }

    @Test
    void findById_shouldBypassOwnership_whenAdmin() {
        Quote quote = new Quote();
        QuoteResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(true);
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        QuoteResponse result = quoteService.findById(1L);

        assertThat(result).isEqualTo(response);
        verify(quoteRepository, never()).findByIdAndActiveTrueAndCreatedById(any(), any());
    }

    // --- findByClientId ---

    @Test
    void findByClientId_shouldReturnMappedList_scopedToOwner() {
        Quote quote = new Quote();
        QuoteResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByClientIdAndActiveTrueAndCreatedById(10L, OWNER_ID)).thenReturn(List.of(quote));
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        List<QuoteResponse> result = quoteService.findByClientId(10L);

        assertThat(result).hasSize(1).contains(response);
    }

    // --- findByReferenceCode ---

    @Test
    void findByReferenceCode_shouldReturnResponse_whenOwnedByCurrentUser() {
        Quote quote = new Quote();
        QuoteResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByReferenceCodeAndActiveTrueAndCreatedById("REF-001", OWNER_ID)).thenReturn(Optional.of(quote));
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        QuoteResponse result = quoteService.findByReferenceCode("REF-001");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findByReferenceCode_shouldThrow404_whenOwnedByAnotherUser() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByReferenceCodeAndActiveTrueAndCreatedById("NOPE", OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.findByReferenceCode("NOPE"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- create ---

    @Test
    void create_shouldSaveQuoteWithClientAndCreator() {
        QuoteRequest request = new QuoteRequest(10L);
        Client client = new Client();
        Quote savedQuote = new Quote();
        QuoteResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(clientRepository.findByIdAndActiveTrueAndCreatedById(10L, OWNER_ID)).thenReturn(Optional.of(client));
        when(quoteRepository.save(any(Quote.class))).thenReturn(savedQuote);
        when(quoteMapper.toResponse(savedQuote)).thenReturn(response);

        QuoteResponse result = quoteService.create(request);

        assertThat(result).isEqualTo(response);
        verify(quoteRepository).save(any(Quote.class));
    }

    @Test
    void create_shouldThrow404_whenClientOwnedByAnotherUser() {
        QuoteRequest request = new QuoteRequest(99L);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(clientRepository.findByIdAndActiveTrueAndCreatedById(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Client not found: 99");
    }

    // --- finalize ---

    @Test
    void finalize_shouldSetSequenceAndReference_whenDraft() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.DRAFT);
        quote.setItems(new ArrayList<>());
        QuoteResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(quoteRepository.findMaxDailySequenceByUserAndDate(OWNER_ID, LocalDate.now())).thenReturn(0);
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        QuoteResponse result = quoteService.finalize(1L);

        assertThat(result).isEqualTo(response);
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.FINALIZED);
        assertThat(quote.getReferenceCode()).isNotNull().contains("-42-");
    }

    @Test
    void finalize_shouldRealignDateToToday_evenIfQuoteWasCreatedEarlier() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.DRAFT);
        quote.setDate(LocalDate.of(2020, 1, 1));
        quote.setItems(new ArrayList<>());
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(quoteRepository.findMaxDailySequenceByUserAndDate(OWNER_ID, LocalDate.now())).thenReturn(0);
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteMapper.toResponse(quote)).thenReturn(dummyResponse());

        quoteService.finalize(1L);

        assertThat(quote.getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void finalize_shouldRecomputeTotalsWithVat() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.DRAFT);
        QuoteItem item = QuoteItem.builder()
                .active(true)
                .unitPrice(new BigDecimal("100.00"))
                .vatRate(new BigDecimal("20"))
                .totalPrice(new BigDecimal("100.00"))
                .build();
        quote.setItems(new ArrayList<>(List.of(item)));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(quoteRepository.findMaxDailySequenceByUserAndDate(OWNER_ID, LocalDate.now())).thenReturn(0);
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteMapper.toResponse(quote)).thenReturn(dummyResponse());

        quoteService.finalize(1L);

        assertThat(quote.getTotalPriceHt()).isEqualByComparingTo("100.00");
        assertThat(quote.getTotalVat()).isEqualByComparingTo("20.00");
        assertThat(quote.getTotalPriceTtc()).isEqualByComparingTo("120.00");
    }

    @Test
    void finalize_shouldThrow_whenAlreadyFinalized() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.FINALIZED);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> quoteService.finalize(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Quote already finalized");
    }

    @Test
    void finalize_shouldThrow_whenCancelled() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.CANCELLED);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> quoteService.finalize(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot finalize a cancelled quote");
    }

    @Test
    void finalize_shouldThrow404_whenOwnedByAnotherUser() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.finalize(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- pending ---

    @Test
    void pending_shouldSetStatus_whenNotCancelled() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.DRAFT);
        QuoteResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        QuoteResponse result = quoteService.pending(1L);

        assertThat(result).isEqualTo(response);
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.PENDING);
    }

    @Test
    void pending_shouldThrow_whenCancelled() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.CANCELLED);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> quoteService.pending(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot set a cancelled quote to pending");
    }

    // --- cancel ---

    @Test
    void cancel_shouldSetStatus_whenNotFinalized() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.PENDING);
        QuoteResponse response = dummyResponse();
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        QuoteResponse result = quoteService.cancel(1L);

        assertThat(result).isEqualTo(response);
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrow_whenFinalized() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.FINALIZED);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> quoteService.cancel(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot cancel a finalized quote");
    }

    // --- delete ---

    @Test
    void delete_shouldDeactivateQuoteAndItems_whenOwnedByCurrentUser() {
        Quote quote = new Quote();
        quote.setActive(true);
        QuoteItem item = new QuoteItem();
        item.setActive(true);
        quote.setItems(new ArrayList<>(List.of(item)));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        quoteService.delete(1L);

        assertThat(quote.isActive()).isFalse();
        assertThat(item.isActive()).isFalse();
        verify(quoteRepository).save(quote);
    }

    @Test
    void delete_shouldThrow_whenFinalized() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.FINALIZED);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> quoteService.delete(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot delete a finalized quote");
    }

    @Test
    void delete_shouldThrow404_whenOwnedByAnotherUser() {
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(quoteRepository.findByIdAndCreatedById(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_shouldBypassOwnership_whenAdmin() {
        Quote quote = new Quote();
        quote.setActive(true);
        quote.setItems(new ArrayList<>());
        when(securityUtils.isAdmin()).thenReturn(true);
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

        quoteService.delete(1L);

        assertThat(quote.isActive()).isFalse();
        verify(quoteRepository, never()).findByIdAndCreatedById(any(), any());
    }

    private QuoteResponse dummyResponse() {
        return new QuoteResponse(1L, "REF", LocalDate.now(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                QuoteStatus.DRAFT, null, List.of());
    }
}
