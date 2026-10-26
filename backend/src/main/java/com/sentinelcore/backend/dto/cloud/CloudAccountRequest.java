package com.sentinelcore.backend.dto.cloud;

import com.sentinelcore.backend.cloud.CloudModels.AuthMode;
import com.sentinelcore.backend.cloud.CloudProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Registration / update payload for a cloud account. Deliberately has no fields for access keys or
 * secrets: the platform authenticates with its own identity and optional STS role assumption.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudAccountRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    private CloudProvider provider;

    @NotBlank
    @Size(max = 64)
    private String externalAccountId;

    @NotNull
    private AuthMode authMode;

    @Size(max = 2048)
    private String roleArn;

    /** Optional STS ExternalId. Write-only: never returned by the API. Send null to keep the current value. */
    @Size(max = 1224)
    @Pattern(regexp = "^[\\w+=,.@:/-]*$", message = "contains invalid characters")
    private String externalId;

    @Size(max = 30)
    private List<@Pattern(regexp = "^[a-z]{2}(-gov|-iso[a-z]*)?-[a-z]+-\\d{1,2}$", message = "is not a valid region code") String> regions;
}
