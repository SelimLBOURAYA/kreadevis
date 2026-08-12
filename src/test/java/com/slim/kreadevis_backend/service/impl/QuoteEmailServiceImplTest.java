package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.config.EmailProperties;
import com.slim.kreadevis_backend.dto.email.EmailMessage;
import com.slim.kreadevis_backend.dto.quote.SendQuoteRequest;
import com.slim.kreadevis_backend.dto.quote.SendQuoteResponse;
import com.slim.kreadevis_backend.entity.Client;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.User;
import com.slim.kreadevis_backend.exception.UnprocessableEntityException;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import com.slim.kreadevis_backend.security.SecurityUtilsTestSupport;
import com.slim.kreadevis_backend.service.EmailService;
import com.slim.kreadevis_backend.service.PdfService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteEmailServiceImplTest {

    private static final Long OWNER_ID = 42L;

    @Mock private QuoteRepository quoteRepository;
    @Mock private PdfService pdfService;
    @Mock private EmailService emailService;
    @Mock private SpringTemplateEngine templateEngine;
    @Mock private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        User currentUser = new User();
        currentUser.setId(OWNER_ID);
        lenient().when(securityUtils.isAdmin()).thenReturn(false);
        lenient().when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        SecurityUtilsTestSupport.wireResolveOwned(securityUtils);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private QuoteEmailServiceImpl service(boolean enabled) {
        EmailProperties props = new EmailProperties(
                enabled,
                new EmailProperties.Mailjet("https://api.mailjet.com/v3.1/send", "k", "s"),
                new EmailProperties.Sender("noreply@test.local", "Kreadevis"));
        return new QuoteEmailServiceImpl(quoteRepository, pdfService, emailService, props, templateEngine, securityUtils);
    }

    private Quote quoteWithClientEmail(String email) {
        Client client = Client.builder().firstName("Jane").lastName("Doe").email(email).build();
        return Quote.builder().id(1L).referenceCode("010824-1-001").client(client).build();
    }

    private void authenticate(String login) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a"));
    }

    @Test
    void sendQuoteToClient_shouldSendPdfAndPersistTrace() {
        authenticate("merchant");
        Quote quote = quoteWithClientEmail("client@example.com");
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(pdfService.generateQuotePdf(1L)).thenReturn(new byte[]{1, 2, 3});
        when(templateEngine.process(eq("email/quote"), any())).thenReturn("<html>body</html>");

        SendQuoteResponse response = service(true).sendQuoteToClient(1L, null);

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(captor.capture());
        EmailMessage sent = captor.getValue();
        assertThat(sent.to()).isEqualTo("client@example.com");
        assertThat(sent.attachment()).isNotNull();
        assertThat(sent.attachment().contentType()).isEqualTo("application/pdf");

        assertThat(response.sentTo()).isEqualTo("client@example.com");
        assertThat(response.sentAt()).isNotNull();
        assertThat(quote.getSentTo()).isEqualTo("client@example.com");
        assertThat(quote.getSentAt()).isEqualTo(response.sentAt());
        verify(quoteRepository).save(quote);
    }

    @Test
    void sendQuoteToClient_shouldUseRecipientOverride() {
        authenticate("merchant");
        Quote quote = quoteWithClientEmail("client@example.com");
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));
        when(pdfService.generateQuotePdf(1L)).thenReturn(new byte[]{1});
        when(templateEngine.process(eq("email/quote"), any())).thenReturn("<html></html>");

        SendQuoteResponse response = service(true)
                .sendQuoteToClient(1L, new SendQuoteRequest("override@example.com", "Merci"));

        assertThat(response.sentTo()).isEqualTo("override@example.com");
        assertThat(quote.getSentTo()).isEqualTo("override@example.com");
    }

    @Test
    void sendQuoteToClient_shouldThrow409_whenEmailDisabled() {
        assertThatThrownBy(() -> service(false).sendQuoteToClient(1L, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
        verify(emailService, never()).send(any());
    }

    @Test
    void sendQuoteToClient_shouldThrow422_whenNoRecipient() {
        Quote quote = quoteWithClientEmail(null);
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> service(true).sendQuoteToClient(1L, new SendQuoteRequest(null, null)))
                .isInstanceOf(UnprocessableEntityException.class);
        verify(emailService, never()).send(any());
    }

    @Test
    void sendQuoteToClient_shouldThrow404_whenQuoteOwnedByAnotherUser() {
        when(quoteRepository.findByIdAndActiveTrueAndCreatedById(1L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(true).sendQuoteToClient(1L, null))
                .isInstanceOf(EntityNotFoundException.class);
        verify(emailService, never()).send(any());
    }

    @Test
    void sendQuoteToClient_shouldBypassOwnership_whenAdmin() {
        when(securityUtils.isAdmin()).thenReturn(true);
        Quote quote = quoteWithClientEmail("client@example.com");
        when(quoteRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(quote));
        when(pdfService.generateQuotePdf(1L)).thenReturn(new byte[]{1});
        when(templateEngine.process(eq("email/quote"), any())).thenReturn("<html></html>");

        service(true).sendQuoteToClient(1L, null);

        verify(quoteRepository, never()).findByIdAndActiveTrueAndCreatedById(any(), any());
    }
}
