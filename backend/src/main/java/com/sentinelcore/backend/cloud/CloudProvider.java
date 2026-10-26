package com.sentinelcore.backend.cloud;

/**
 * Cloud providers known to the platform. Only providers with a registered
 * {@link CloudProviderConnector} are usable; see {@link CloudConnectorRegistry}.
 */
public enum CloudProvider {
    AWS,
    AZURE,
    GCP
}
