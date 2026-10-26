package com.sentinelcore.backend.dto.cloud;

import com.sentinelcore.backend.cloud.CloudModels.AuthMode;
import com.sentinelcore.backend.cloud.CloudProvider;
import com.sentinelcore.backend.entity.CloudAccount;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudAccountDTO {

    private Long id;

    private String name;

    private CloudProvider provider;

    private String externalAccountId;

    private AuthMode authMode;

    private String roleArn;

    /** Whether an STS ExternalId is configured. The value itself is never returned. */
    private boolean externalIdConfigured;

    private List<String> regions;

    private CloudAccount.ConnectionStatus connectionStatus;

    private LocalDateTime lastVerifiedAt;

    private String lastVerificationMessage;

    private LocalDateTime lastScanAt;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
