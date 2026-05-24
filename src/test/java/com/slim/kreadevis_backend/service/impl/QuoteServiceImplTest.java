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
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
    @Mock private UserRepository userRepository;
    @Mock private QuoteMapper quoteMapper;
    @InjectMocks private QuoteServiceImpl quoteService;

    // --- findAll ---

    @Test
    void findAll_shouldReturnMappedList() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        Quote quote = new Quote();
        QuoteResponse response = dummyResponse();
        when(quoteRepository.findByDateRange(start, end)).thenReturn(List.of(quote));
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        List<QuoteResponse> result = quoteService.findAll(start, end);

        assertThat(result).hasSize(1).contains(response);
    }

    // --- findById ---

    @Test
    void findById_shouldReturnResponse_whenFound() {
        Quote quote = new Quote();
        QuoteResponse response = dummyResponse();
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        QuoteResponse result = quoteService.findById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(quoteRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Quote not found: 99");
    }

    // --- findByClientId ---

    @Test
    void findByClientId_shouldReturnMappedList() {
        Quote quote = new Quote();
        QuoteResponse response = dummyResponse();
        when(quoteRepository.findByClientIdAndActiveTrue(10L)).thenReturn(List.of(quote));
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        List<QuoteResponse> result = quoteService.findByClientId(10L);

        assertThat(result).hasSize(1).contains(response);
    }

    // --- findByReferenceCode ---

    @Test
    void findByReferenceCode_shouldReturnResponse_whenFound() {
        Quote quote = new Quote();
        QuoteResponse response = dummyResponse();
        when(quoteRepository.findByReferenceCodeAndActiveTrue("REF-001")).thenReturn(Optional.of(quote));
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        QuoteResponse result = quoteService.findByReferenceCode("REF-001");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findByReferenceCode_shouldThrow_whenNotFound() {
        when(quoteRepository.findByReferenceCodeAndActiveTrue("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.findByReferenceCode("NOPE"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- create ---

    @Test
    void create_shouldSaveQuoteWithClientAndCreator() throws Exception {
        try (var mocks = setupSecurityContext()) {

        QuoteRequest request = new QuoteRequest(10L);
        Client client = new Client();
        Quote savedQuote = new Quote();
        QuoteResponse response = dummyResponse();
        when(clientRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(client));
        when(quoteRepository.save(any(Quote.class))).thenReturn(savedQuote);
        when(quoteMapper.toResponse(savedQuote)).thenReturn(response);

        QuoteResponse result = quoteService.create(request);

        assertThat(result).isEqualTo(response);
        verify(quoteRepository).save(any(Quote.class));
        }
    }

    @Test
    void create_shouldThrow_whenClientNotFound() {
        QuoteRequest request = new QuoteRequest(99L);
        when(clientRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Client not found: 99");
    }

    // --- finalize ---

    @Test
    void finalize_shouldSetSequenceAndReference_whenDraft() throws Exception {
        try (var mocks = setupSecurityContext()) {

        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.DRAFT);
        quote.setItems(new ArrayList<>());
        QuoteResponse response = dummyResponse();
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));
        when(quoteRepository.findMaxDailySequenceByUserAndDate(42L, LocalDate.now())).thenReturn(0);
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteMapper.toResponse(quote)).thenReturn(response);

        QuoteResponse result = quoteService.finalize(1L);

        assertThat(result).isEqualTo(response);
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.FINALIZED);
        assertThat(quote.getReferenceCode()).isNotNull().contains("-42-");
        }
    }

    @Test
    void finalize_shouldRealignDateToToday_evenIfQuoteWasCreatedEarlier() throws Exception {
        try (var mocks = setupSecurityContext()) {

        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.DRAFT);
        quote.setDate(LocalDate.of(2020, 1, 1));
        quote.setItems(new ArrayList<>());
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));
        when(quoteRepository.findMaxDailySequenceByUserAndDate(42L, LocalDate.now())).thenReturn(0);
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteMapper.toResponse(quote)).thenReturn(dummyResponse());

        quoteService.finalize(1L);

        assertThat(quote.getDate()).isEqualTo(LocalDate.now());
        }
    }

    @Test
    void finalize_shouldRecomputeTotalsWithVat() throws Exception {
        try (var mocks = setupSecurityContext()) {

        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.DRAFT);
        QuoteItem item = QuoteItem.builder()
                .active(true)
                .unitPrice(new BigDecimal("100.00"))
                .vatRate(new BigDecimal("20"))
                .totalPrice(new BigDecimal("100.00"))
                .build();
        quote.setItems(new ArrayList<>(List.of(item)));
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));
        when(quoteRepository.findMaxDailySequenceByUserAndDate(42L, LocalDate.now())).thenReturn(0);
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(quoteMapper.toResponse(quote)).thenReturn(dummyResponse());

        quoteService.finalize(1L);

        assertThat(quote.getTotalPriceHt()).isEqualByComparingTo("100.00");
        assertThat(quote.getTotalVat()).isEqualByComparingTo("20.00");
        assertThat(quote.getTotalPriceTtc()).isEqualByComparingTo("120.00");
        }
    }

    @Test
    void finalize_shouldThrow_whenAlreadyFinalized() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.FINALIZED);
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> quoteService.finalize(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Quote already finalized");
    }

    @Test
    void finalize_shouldThrow_whenCancelled() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.CANCELLED);
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> quoteService.finalize(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot finalize a cancelled quote");
    }

    // --- pending ---

    @Test
    void pending_shouldSetStatus_whenNotCancelled() {
        Quote quote = new Quote();
        quote.setStatus(QuoteStatus.DRAFT);
        QuoteResponse response = dummyResponse();
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));
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
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));

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
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));
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
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> quoteService.cancel(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot cancel a finalized quote");
    }

    // --- delete ---

    @Test
    void delete_shouldDeactivateQuoteAndItems_whenFound() {
        Quote quote = new Quote();
        quote.setActive(true);
        QuoteItem item = new QuoteItem();
        item.setActive(true);
        quote.setItems(new ArrayList<>(List.of(item)));
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

        quoteService.delete(1L);

        assertThat(quote.isActive()).isFalse();
        assertThat(item.isActive()).isFalse();
        verify(quoteRepository).save(quote);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(quoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quoteService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- helpers ---

    /**
     * Sets up the SecurityContextHolder static mock, authentication, and user repository
     * for tests that call getCurrentUser() (create, finalize).
     * Caller must call .close() on the returned AutoCloseable after the test.
     */
    private AutoCloseable setupSecurityContext() {
        var securityHolderMock = mockStatic(SecurityContextHolder.class);
        var securityContext = mock(SecurityContext.class);
        var authentication = mock(Authentication.class);
        var userDetails = mock(UserDetailsImpl.class);

        securityHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(42L);

        User currentUser = new User();
        currentUser.setId(42L);
        currentUser.setLogin("testuser");
        when(userRepository.findById(42L)).thenReturn(Optional.of(currentUser));

        return securityHolderMock;
    }

    private QuoteResponse dummyResponse() {
        return new QuoteResponse(1L, "REF", LocalDate.now(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                QuoteStatus.DRAFT, null, List.of());
    }
}
