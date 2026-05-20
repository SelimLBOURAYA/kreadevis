package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.config.AppProperties;
import com.slim.kreadevis_backend.entity.*;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceImplTest {

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private Resource logoResource;

    private PdfServiceImpl pdfService;

    @BeforeEach
    void setUp() {
        AppProperties.Company company = new AppProperties.Company(
                "Test Company", "1 Rue Test", "01 23 45 67 89", "test@company.fr", "123456789");
        AppProperties.DocumentConfig doc = new AppProperties.DocumentConfig(
                "classpath:static/logo.png", "/tmp/test", "/tmp/test/factures");
        AppProperties props = new AppProperties(company, doc);
        pdfService = new PdfServiceImpl(quoteRepository, props, resourceLoader);
    }

    @Test
    void generateQuotePdf_returnsNonEmptyBytes() {
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(buildSampleQuote()));
        when(resourceLoader.getResource("classpath:static/logo.png")).thenReturn(logoResource);
        when(logoResource.exists()).thenReturn(false);

        byte[] result = pdfService.generateQuotePdf(1L);

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void generateInvoicePdf_returnsNonEmptyBytes() {
        when(quoteRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(buildSampleQuote()));
        when(resourceLoader.getResource("classpath:static/logo.png")).thenReturn(logoResource);
        when(logoResource.exists()).thenReturn(false);

        byte[] result = pdfService.generateInvoicePdf(2L);

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void generateQuotePdf_throwsWhenQuoteNotFound() {
        when(quoteRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdfService.generateQuotePdf(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void generateInvoicePdf_throwsWhenQuoteNotFound() {
        when(quoteRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdfService.generateInvoicePdf(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void generateQuotePdf_worksWithoutLogo() throws IOException {
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(buildSampleQuote()));
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
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));
        when(resourceLoader.getResource("classpath:static/logo.png")).thenReturn(logoResource);
        when(logoResource.exists()).thenReturn(false);

        byte[] result = pdfService.generateQuotePdf(1L);

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    void generateQuotePdf_skipsInactiveItems() {
        Quote quote = buildSampleQuote();
        quote.getItems().get(0).setActive(false);
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));
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
                .unitPrice(85.00f)
                .vatRate(20f)
                .build();

        QuoteItem item = QuoteItem.builder()
                .id(1L)
                .quantity(2L)
                .unitPrice(85.00f)
                .totalPrice(170.00f)
                .product(product)
                .active(true)
                .build();

        return Quote.builder()
                .id(1L)
                .referenceCode("200526-1-001")
                .date(LocalDate.of(2026, 5, 20))
                .status(QuoteStatus.FINALIZED)
                .totalPrice(170.00f)
                .client(client)
                .items(new java.util.ArrayList<>(List.of(item)))
                .active(true)
                .build();
    }
}
