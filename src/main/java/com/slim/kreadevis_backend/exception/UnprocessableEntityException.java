package com.slim.kreadevis_backend.exception;

/**
 * Thrown when a request is well-formed but cannot be processed because of a
 * missing business precondition (mapped to HTTP 422).
 */
public class UnprocessableEntityException extends RuntimeException {

    public UnprocessableEntityException(String message) {
        super(message);
    }
}
