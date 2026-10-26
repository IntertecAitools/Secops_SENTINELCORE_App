package com.sentinelcore.backend.dto.cloud;

import com.sentinelcore.backend.cloud.CloudProvider;
import com.sentinelcore.backend.cloud.RemediationActionType;
import lombok.*;

import java.util.List;

/**
 * What the platform can actually do in this deployment, so the UI never implies unsupported features.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudCapabilitiesDTO {

    private List<CloudProvider> supportedProviders;

    private List<CloudProvider> knownButUnsupportedProviders;

    private boolean awsIntegrationEnabled;

    private boolean remediationExecutionEnabled;

    private List<RemediationActionType> remediationActions;
}
