package com.sentinelcore.backend.cloud.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AWS integration settings. Contains no credentials: the platform identity is resolved through the
 * standard AWS credential provider chain (environment, web identity, SSO, instance/container role).
 */
@Component
public class AwsSettings {

    private final boolean enabled;
    private final String defaultRegion;
    private final Duration apiCallTimeout;
    private final Duration apiCallAttemptTimeout;
    private final int assumeRoleSessionSeconds;

    public AwsSettings(
            @Value("${app.cloud.aws.enabled:false}") boolean enabled,
            @Value("${app.cloud.aws.default-region:us-east-1}") String defaultRegion,
            @Value("${app.cloud.aws.api-call-timeout-seconds:30}") long apiCallTimeoutSeconds,
            @Value("${app.cloud.aws.api-call-attempt-timeout-seconds:10}") long apiCallAttemptTimeoutSeconds,
            @Value("${app.cloud.aws.assume-role-session-seconds:900}") int assumeRoleSessionSeconds) {
        if (apiCallTimeoutSeconds <= 0 || apiCallAttemptTimeoutSeconds <= 0) {
            throw new IllegalStateException("AWS API timeouts must be positive");
        }
        if (assumeRoleSessionSeconds < 900 || assumeRoleSessionSeconds > 3600) {
            throw new IllegalStateException("app.cloud.aws.assume-role-session-seconds must be between 900 and 3600");
        }
        this.enabled = enabled;
        this.defaultRegion = defaultRegion;
        this.apiCallTimeout = Duration.ofSeconds(apiCallTimeoutSeconds);
        this.apiCallAttemptTimeout = Duration.ofSeconds(apiCallAttemptTimeoutSeconds);
        this.assumeRoleSessionSeconds = assumeRoleSessionSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public Duration getApiCallTimeout() {
        return apiCallTimeout;
    }

    public Duration getApiCallAttemptTimeout() {
        return apiCallAttemptTimeout;
    }

    public int getAssumeRoleSessionSeconds() {
        return assumeRoleSessionSeconds;
    }
}
