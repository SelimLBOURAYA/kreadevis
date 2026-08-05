package com.slim.kreadevis_backend.service;

import com.slim.kreadevis_backend.dto.email.EmailMessage;

public interface EmailService {

    /**
     * Sends a single email through the configured provider.
     *
     * @param message the email to send
     * @throws com.slim.kreadevis_backend.exception.EmailSendException if the provider rejects the message
     */
    void send(EmailMessage message);
}
