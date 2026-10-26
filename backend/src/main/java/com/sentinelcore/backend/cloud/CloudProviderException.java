package com.sentinelcore.backend.cloud;

/**
 * A classified failure from a cloud provider. The message is safe to show to authorized users:
 * it never contains credentials or raw provider responses.
 */
public class CloudProviderException extends RuntimeException {

    private final CloudErrorType errorType;

    public CloudProviderException(CloudErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public CloudProviderException(CloudErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public CloudErrorType getErrorType() {
        return errorType;
    }
}
