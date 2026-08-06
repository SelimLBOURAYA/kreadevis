package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.dto.quote.QuoteItemRequest;
import com.slim.kreadevis_backend.dto.quote.QuoteItemResponse;
import com.slim.kreadevis_backend.entity.Product;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.QuoteItem;
import com.slim.kreadevis_backend.entity.QuoteStatus;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.mapper.QuoteMapper;
import com.slim.kreadevis_backend.repository.ProductRepository;
import com.slim.kreadevis_backend.repository.QuoteItemRepository;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteItemServiceImplTest {

    @Mock private QuoteRepository quoteRepository;
    @Mock private QuoteItemRepository quoteItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private QuoteMapper quoteMapper;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private QuoteItemServiceImpl service;

    private static final Long OWNER_ID = 42L;

    @BeforeEach
    void setUp() {
        User currentUser = new User();
        currentUser.setId(OWNER_ID);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
    }

    // --- status guards ---

    @Test
    void addItem_shouldThrow_whenQuoteFinalized() {
        Quote quote = quoteWithStatus(QuoteStatus.FINALIZED);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> service.addItem(1L, new QuoteItemRequest(5L, 1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FINALIZED");
    }

    @Test
    void addItem_shouldThrow_whenQuoteCancelled() {
        Quote quote = quoteWithStatus(QuoteStatus.CANCELLED);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> service.addItem(1L, new QuoteItemRequest(5L, 1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void updateItem_shouldThrow_whenQuoteFinalized() {
        Quote quote = quoteWithStatus(QuoteStatus.FINALIZED);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> service.updateItem(1L, 10L, new QuoteItemRequest(5L, 1L)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteItem_shouldThrow_whenQuoteCancelled() {
        Quote quote = quoteWithStatus(QuoteStatus.CANCELLED);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> service.deleteItem(1L, 10L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addItem_shouldThrow404_whenQuoteOwnedByAnotherUser() {
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItem(1L, new QuoteItemRequest(5L, 1L)))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    // --- snapshot VAT + recompute totals ---

    @Test
    void addItem_shouldSnapshotVatRateFromProduct_andRecomputeTotals() {
        Quote quote = quoteWithStatus(QuoteStatus.DRAFT);
        Product product = Product.builder()
                .id(5L)
                .unitPrice(new BigDecimal("100.00"))
                .vatRate(new BigDecimal("20"))
                .build();
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(productRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(product));
        when(quoteItemRepository.save(any(QuoteItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quoteMapper.toItemResponse(any(QuoteItem.class)))
                .thenReturn(new QuoteItemResponse(null, null, 2L, new BigDecimal("100.00"),
                        new BigDecimal("20"), new BigDecimal("200.00")));

        QuoteItemResponse result = service.addItem(1L, new QuoteItemRequest(5L, 2L));

        ArgumentCaptor<QuoteItem> itemCaptor = ArgumentCaptor.forClass(QuoteItem.class);
        verify(quoteItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getVatRate()).isEqualByComparingTo("20");
        assertThat(itemCaptor.getValue().getTotalPrice()).isEqualByComparingTo("200.00");

        assertThat(quote.getTotalPriceHt()).isEqualByComparingTo("200.00");
        assertThat(quote.getTotalVat()).isEqualByComparingTo("40.00");
        assertThat(quote.getTotalPriceTtc()).isEqualByComparingTo("240.00");
        verify(quoteRepository).save(quote);
        assertThat(result.vatRate()).isEqualByComparingTo("20");
    }

    @Test
    void deleteItem_shouldExcludeFromTotals() {
        Quote quote = quoteWithStatus(QuoteStatus.DRAFT);
        quote.setId(1L);
        QuoteItem item = QuoteItem.builder()
                .id(10L)
                .active(true)
                .quote(quote)
                .unitPrice(new BigDecimal("50.00"))
                .vatRate(new BigDecimal("10"))
                .totalPrice(new BigDecimal("100.00"))
                .build();
        quote.getItems().add(item);

        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(quoteItemRepository.findById(10L)).thenReturn(Optional.of(item));

        service.deleteItem(1L, 10L);

        assertThat(item.isActive()).isFalse();
        assertThat(quote.getTotalPriceHt()).isEqualByComparingTo("0");
        assertThat(quote.getTotalVat()).isEqualByComparingTo("0");
        assertThat(quote.getTotalPriceTtc()).isEqualByComparingTo("0");
    }

    @Test
    void addItem_shouldComputeMixedVatRates() {
        Quote quote = quoteWithStatus(QuoteStatus.DRAFT);
        QuoteItem existing = QuoteItem.builder()
                .id(10L)
                .active(true)
                .unitPrice(new BigDecimal("100.00"))
                .vatRate(new BigDecimal("5.5"))
                .totalPrice(new BigDecimal("100.00"))
                .build();
        quote.getItems().add(existing);

        Product newProduct = Product.builder()
                .id(6L)
                .unitPrice(new BigDecimal("200.00"))
                .vatRate(new BigDecimal("20"))
                .build();
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(productRepository.findByIdAndActiveTrue(6L)).thenReturn(Optional.of(newProduct));
        when(quoteItemRepository.save(any(QuoteItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(quoteMapper.toItemResponse(any(QuoteItem.class)))
                .thenReturn(new QuoteItemResponse(null, null, 1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        service.addItem(1L, new QuoteItemRequest(6L, 1L));

        // total HT = 100 + 200 = 300
        // TVA = (100 * 5.5 / 100) + (200 * 20 / 100) = 5.50 + 40.00 = 45.50
        // TTC = 345.50
        assertThat(quote.getTotalPriceHt()).isEqualByComparingTo("300.00");
        assertThat(quote.getTotalVat()).isEqualByComparingTo("45.50");
        assertThat(quote.getTotalPriceTtc()).isEqualByComparingTo("345.50");
    }

    // --- item ownership ---

    @Test
    void updateItem_shouldThrow_whenItemDoesNotBelongToQuote() {
        Quote quote = quoteWithStatus(QuoteStatus.DRAFT);
        quote.setId(1L);
        Quote otherQuote = new Quote();
        otherQuote.setId(2L);
        QuoteItem item = QuoteItem.builder()
                .id(10L)
                .active(true)
                .quote(otherQuote)
                .build();
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(quoteItemRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.updateItem(1L, 10L, new QuoteItemRequest(5L, 1L)))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("does not belong to quote");
    }

    @Test
    void deleteItem_shouldThrow_whenItemDoesNotBelongToQuote() {
        Quote quote = quoteWithStatus(QuoteStatus.DRAFT);
        quote.setId(1L);
        Quote otherQuote = new Quote();
        otherQuote.setId(2L);
        QuoteItem item = QuoteItem.builder()
                .id(10L)
                .active(true)
                .quote(otherQuote)
                .build();
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(quoteItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.deleteItem(1L, 10L))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("does not belong to quote");
    }

    // --- helpers ---

    private Quote quoteWithStatus(QuoteStatus status) {
        Quote quote = new Quote();
        quote.setStatus(status);
        quote.setItems(new ArrayList<>(List.<QuoteItem>of()));
        return quote;
    }
}
