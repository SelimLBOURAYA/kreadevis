package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.config.EmailProperties;
import com.slim.kreadevis_backend.dto.email.EmailAttachment;
import com.slim.kreadevis_backend.dto.email.EmailMessage;
import com.slim.kreadevis_backend.dto.quote.SendQuoteRequest;
import com.slim.kreadevis_backend.dto.quote.SendQuoteResponse;
import com.slim.kreadevis_backend.entity.Client;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.exception.UnprocessableEntityException;
import com.slim.kreadevis_backend.repository.QuoteRepository;
import com.slim.kreadevis_backend.security.SecurityUtils;
import com.slim.kreadevis_backend.service.EmailService;
import com.slim.kreadevis_backend.service.PdfService;
import com.slim.kreadevis_backend.service.QuoteEmailService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class QuoteEmailServiceImpl implements QuoteEmailService {

    private static final String TEMPLATE = "email/quote";

    private final QuoteRepository quoteRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final EmailProperties emailProperties;
    private final SpringTemplateEngine templateEngine;
    private final SecurityUtils securityUtils;

    public QuoteEmailServiceImpl(QuoteRepository quoteRepository,
                                 PdfService pdfService,
                                 EmailService emailService,
                                 EmailProperties emailProperties,
                                 SpringTemplateEngine templateEngine,
                                 SecurityUtils securityUtils) {
        this.quoteRepository = quoteRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
        this.templateEngine = templateEngine;
        this.securityUtils = securityUtils;
    }

    @Override
    @Transactional
    public SendQuoteResponse sendQuoteToClient(Long quoteId, SendQuoteRequest request) {
        if (!emailProperties.enabled()) {
            throw new IllegalStateException("Email sending is disabled");
        }

        Quote quote = securityUtils.isAdmin()
                ? quoteRepository.findByIdAndActiveTrue(quoteId)
                        .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + quoteId))
                : quoteRepository.findByIdAndActiveTrueAndCreatedById(quoteId, securityUtils.getCurrentUser().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + quoteId));

        String recipient = resolveRecipient(quote, request);

        byte[] pdf = pdfService.generateQuotePdf(quoteId);
        EmailAttachment attachment = new EmailAttachment(
                "quote-" + quoteId + ".pdf",
                "application/pdf",
                Base64.getEncoder().encodeToString(pdf));

        String htmlBody = renderBody(quote, request);
        String subject = "Votre devis"
                + (quote.getReferenceCode() != null ? " " + quote.getReferenceCode() : "");

        emailService.send(new EmailMessage(
                recipient, clientDisplayName(quote.getClient()), subject, htmlBody, attachment));

        LocalDateTime sentAt = LocalDateTime.now();
        quote.setSentAt(sentAt);
        quote.setSentTo(recipient);
        quoteRepository.save(quote);

        return new SendQuoteResponse(sentAt, recipient);
    }

    private String resolveRecipient(Quote quote, SendQuoteRequest request) {
        if (request != null && request.recipientOverride() != null && !request.recipientOverride().isBlank()) {
            return request.recipientOverride();
        }
        Client client = quote.getClient();
        String clientEmail = client != null ? client.getEmail() : null;
        if (clientEmail == null || clientEmail.isBlank()) {
            throw new UnprocessableEntityException(
                    "Quote client has no email address and no recipient override was provided");
        }
        return clientEmail;
    }

    private String renderBody(Quote quote, SendQuoteRequest request) {
        Context context = new Context();
        context.setVariable("merchantName", currentMerchantName());
        context.setVariable("referenceCode", quote.getReferenceCode());
        context.setVariable("clientName", clientDisplayName(quote.getClient()));
        context.setVariable("customMessage",
                request != null ? request.customMessage() : null);
        return templateEngine.process(TEMPLATE, context);
    }

    private String currentMerchantName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private String clientDisplayName(Client client) {
        if (client == null) {
            return null;
        }
        String first = client.getFirstName() != null ? client.getFirstName() + " " : "";
        return (first + (client.getLastName() != null ? client.getLastName() : "")).trim();
    }
}
