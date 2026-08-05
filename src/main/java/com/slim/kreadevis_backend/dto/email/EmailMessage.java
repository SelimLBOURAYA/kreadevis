package com.slim.kreadevis_backend.dto.email;

/**
 * Provider-agnostic representation of a single outgoing email.
 *
 * @param to          recipient email address
 * @param toName      recipient display name (may be {@code null})
 * @param subject     email subject
 * @param htmlBody    rendered HTML body
 * @param attachment  optional attachment (may be {@code null})
 */
public record EmailMessage(String to, String toName, String subject, String htmlBody, EmailAttachment attachment) {}
