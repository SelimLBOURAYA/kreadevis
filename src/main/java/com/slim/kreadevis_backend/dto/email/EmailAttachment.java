package com.slim.kreadevis_backend.dto.email;

/**
 * A file to attach to an outgoing email.
 *
 * @param filename      name shown to the recipient
 * @param contentType   MIME type (e.g. {@code application/pdf})
 * @param base64Content file bytes, Base64-encoded
 */
public record EmailAttachment(String filename, String contentType, String base64Content) {}
