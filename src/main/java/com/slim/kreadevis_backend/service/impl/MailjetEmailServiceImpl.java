package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.config.EmailProperties;
import com.slim.kreadevis_backend.dto.email.EmailAttachment;
import com.slim.kreadevis_backend.dto.email.EmailMessage;
import com.slim.kreadevis_backend.dto.email.MailjetPayload;
import com.slim.kreadevis_backend.exception.EmailSendException;
import com.slim.kreadevis_backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Sends emails through the Mailjet Send API v3.1 over HTTPS.
 * Never logs credentials: only the resulting HTTP status is logged.
 */
@Service
public class MailjetEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(MailjetEmailServiceImpl.class);

    private final RestClient mailjetRestClient;
    private final EmailProperties emailProperties;

    public MailjetEmailServiceImpl(RestClient mailjetRestClient, EmailProperties emailProperties) {
        this.mailjetRestClient = mailjetRestClient;
        this.emailProperties = emailProperties;
    }

    @Override
    public void send(EmailMessage message) {
        MailjetPayload payload = buildPayload(message);
        try {
            mailjetRestClient.post()
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Mailjet accepted email to {}", message.to());
        } catch (RestClientException ex) {
            log.warn("Mailjet rejected email to {}: {}", message.to(), ex.getMessage());
            throw new EmailSendException("Email provider failed to send the message", ex);
        }
    }

    private MailjetPayload buildPayload(EmailMessage message) {
        EmailProperties.Sender sender = emailProperties.sender();
        List<MailjetPayload.Attachment> attachments = null;
        EmailAttachment attachment = message.attachment();
        if (attachment != null) {
            attachments = List.of(new MailjetPayload.Attachment(
                    attachment.contentType(),
                    attachment.filename(),
                    attachment.base64Content()));
        }
        MailjetPayload.Message mjMessage = new MailjetPayload.Message(
                new MailjetPayload.Contact(sender.email(), sender.name()),
                List.of(new MailjetPayload.Contact(message.to(), message.toName())),
                message.subject(),
                message.htmlBody(),
                attachments);
        return new MailjetPayload(List.of(mjMessage));
    }
}
