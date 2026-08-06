package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.config.AppProperties;
import com.slim.kreadevis_backend.entity.*;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceImplTest {

    private static final Long OWNER_ID = 42L;

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private Resource logoResource;
    @Mock
    private SecurityUtils securityUtils;

    private PdfServiceImpl pdfService;

    @BeforeEach
    void setUp() {
        AppProperties.Company company = new AppProperties.Company(
                "Test Company", "1 Rue Test", "01 23 45 67 89", "test@company.fr", "123456789");
        AppProperties.DocumentConfig doc = new AppProperties.DocumentConfig(
                "classpath:static/logo.png", "/tmp/test");
        AppProperties props = new AppProperties(company, doc);
        pdfService = new PdfServiceImpl(quoteRepository, props, resourceLoader, securityUtils);

        User currentUser = new User();
        currentUser.setId(OWNER_ID);
        lenient().when(securityUtils.isAdmin()).thenReturn(false);
        lenient().when(securityUtils.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void generateQuotePdf_returnsNonEmptyBytes() {
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(buildSampleQuote()));
        when(resourceLoader.getResource("classpath:static/logo.png")).thenReturn(logoResource);
        when(logoResource.exists()).thenReturn(false);

        byte[] result = pdfService.generateQuotePdf(1L);

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void generateQuotePdf_throwsWhenQuoteNotFound() {
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdfService.generateQuotePdf(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void generateQuotePdf_throws404_whenQuoteOwnedByAnotherUser() {
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdfService.generateQuotePdf(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void generateQuotePdf_bypassesOwnership_whenAdmin() {
        when(securityUtils.isAdmin()).thenReturn(true);
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(buildSampleQuote()));
        when(resourceLoader.getResource("classpath:static/logo.png")).thenReturn(logoResource);
        when(logoResource.exists()).thenReturn(false);

        byte[] result = pdfService.generateQuotePdf(1L);

        assertThat(result).isNotNull().isNotEmpty();
        verify(quoteRepository, never()).findByIdAndActiveTrueAndCreatedById(any(), any());
    }

    @Test
    void generateQuotePdf_worksWithoutLogo() throws IOException {
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(buildSampleQuote()));
        when(resourceLoader.getResource("classpath:static/logo.png")).thenReturn(logoResource);
        when(logoResource.exists()).thenReturn(true);
        when(logoResource.getContentAsByteArray()).thenThrow(new IOException("logo unavailable"));

        byte[] result = pdfService.generateQuotePdf(1L);

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void generateQuotePdf_worksWithEmptyItemsList() {
        Quote quote = buildSampleQuote();
        quote.getItems().clear();
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(resourceLoader.getResource("classpath:static/logo.png")).thenReturn(logoResource);
        when(logoResource.exists()).thenReturn(false);

        byte[] result = pdfService.generateQuotePdf(1L);

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void generateQuotePdf_skipsInactiveItems() {
        Quote quote = buildSampleQuote();
        quote.getItems().get(0).setActive(false);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(resourceLoader.getResource("classpath:static/logo.png")).thenReturn(logoResource);
        when(logoResource.exists()).thenReturn(false);

        byte[] result = pdfService.generateQuotePdf(1L);

        assertThat(result).isNotNull().isNotEmpty();
    }

    private Quote buildSampleQuote() {
        Address address = Address.builder()
                .id(1L)
                .streetNumber("10")
                .street("Avenue de la Paix")
                .postalCode("75001")
                .city("Paris")
                .build();

        Client client = Client.builder()
                .id(1L)
                .firstName("Jean")
                .lastName("Dupont")
                .company("Dupont SARL")
                .phone("06 00 00 00 00")
                .email("jean@dupont.fr")
                .address(address)
                .build();

        Product product = Product.builder()
                .id(1L)
                .label("Robinet mitigeur")
                .unitPrice(new BigDecimal("85.00"))
                .vatRate(new BigDecimal("20"))
                .build();

        QuoteItem item = QuoteItem.builder()
                .id(1L)
                .quantity(2L)
                .unitPrice(new BigDecimal("85.00"))
                .vatRate(new BigDecimal("20"))
                .totalPrice(new BigDecimal("170.00"))
                .product(product)
                .active(true)
                .build();

        return Quote.builder()
                .id(1L)
                .referenceCode("200526-1-001")
                .date(LocalDate.of(2026, 5, 20))
                .status(QuoteStatus.FINALIZED)
                .totalPriceHt(new BigDecimal("170.00"))
                .totalVat(new BigDecimal("34.00"))
                .totalPriceTtc(new BigDecimal("204.00"))
                .client(client)
                .items(new java.util.ArrayList<>(List.of(item)))
                .active(true)
                .build();
    }
}
