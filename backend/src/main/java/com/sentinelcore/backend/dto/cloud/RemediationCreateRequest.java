package com.sentinelcore.backend.dto.cloud;

import com.sentinelcore.backend.cloud.RemediationActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Proposes an allowlisted remediation for a finding. The target resource and account are taken
 * from the finding on the server; they cannot be supplied by the client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationCreateRequest {

    @NotNull
    private Long findingId;

    @NotNull
    private RemediationActionType actionType;

    @Size(max = 1000)
    private String justification;
}
