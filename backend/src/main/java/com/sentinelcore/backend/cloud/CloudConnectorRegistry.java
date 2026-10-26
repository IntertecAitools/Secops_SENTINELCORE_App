package com.sentinelcore.backend.cloud;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the connector for a provider. Providers without a registered connector (currently Azure
 * and GCP) are reported as unsupported rather than silently ignored.
 */
@Component
public class CloudConnectorRegistry {

    private final Map<CloudProvider, CloudProviderConnector> connectors = new EnumMap<>(CloudProvider.class);

    public CloudConnectorRegistry(List<CloudProviderConnector> connectorBeans) {
        for (CloudProviderConnector connector : connectorBeans) {
            if (connectors.put(connector.getProvider(), connector) != null) {
                throw new IllegalStateException("Duplicate connector for provider " + connector.getProvider());
            }
        }
    }

    public CloudProviderConnector get(CloudProvider provider) {
        CloudProviderConnector connector = connectors.get(provider);
        if (connector == null) {
            throw new UnsupportedCloudOperationException(
                    "Cloud provider " + provider + " is not supported yet");
        }
        return connector;
    }

    public boolean isSupported(CloudProvider provider) {
        return connectors.containsKey(provider);
    }

    public Set<CloudProvider> supportedProviders() {
        return connectors.keySet();
    }
}
