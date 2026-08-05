package com.slim.kreadevis_backend.dto.quote;

import java.time.LocalDateTime;

/**
 * Result of a successful quote email send.
 *
 * @param sentAt timestamp of the send
 * @param sentTo email address actually used
 */
public record SendQuoteResponse(LocalDateTime sentAt, String sentTo) {}
