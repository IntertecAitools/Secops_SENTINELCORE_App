package com.sentinelcore.backend.exception;

/**
 * Client error whose message is safe to return to the caller (HTTP 400).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
