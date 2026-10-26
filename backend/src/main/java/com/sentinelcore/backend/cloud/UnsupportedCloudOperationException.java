package com.sentinelcore.backend.cloud;

/**
 * The requested provider or capability is not implemented (HTTP 501).
 */
public class UnsupportedCloudOperationException extends RuntimeException {

    public UnsupportedCloudOperationException(String message) {
        super(message);
    }
}
