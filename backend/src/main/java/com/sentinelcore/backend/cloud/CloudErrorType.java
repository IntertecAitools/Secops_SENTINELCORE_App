package com.sentinelcore.backend.cloud;

/**
 * Provider-neutral classification of cloud API failures.
 */
public enum CloudErrorType {
    /** Integration disabled or platform credentials unavailable. */
    NOT_CONFIGURED,
    /** Account/role configuration is invalid (e.g. credentials resolve to a different account). */
    INVALID_CONFIGURATION,
    ACCESS_DENIED,
    CREDENTIALS_EXPIRED,
    THROTTLED,
    TIMEOUT,
    RESOURCE_NOT_FOUND,
    /** The call may or may not have taken effect (e.g. timeout during a mutation). */
    OUTCOME_UNKNOWN,
    PROVIDER_ERROR
}
