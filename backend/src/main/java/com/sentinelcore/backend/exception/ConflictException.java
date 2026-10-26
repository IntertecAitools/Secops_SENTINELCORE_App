package com.sentinelcore.backend.exception;

/**
 * Request conflicts with the current state of a resource (HTTP 409). Message is safe to return.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
