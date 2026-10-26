package com.sentinelcore.backend.exception;

/**
 * Caller exceeded a rate limit (HTTP 429). Message is safe to return.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
