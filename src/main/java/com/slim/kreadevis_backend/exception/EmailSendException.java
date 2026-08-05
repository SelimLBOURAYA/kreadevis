package com.slim.kreadevis_backend.exception;

/**
 * Thrown when the email provider rejects or fails to accept a message
 * (mapped to HTTP 502).
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
