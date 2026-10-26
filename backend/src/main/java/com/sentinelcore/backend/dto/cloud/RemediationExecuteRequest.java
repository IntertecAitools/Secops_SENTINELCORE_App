package com.sentinelcore.backend.dto.cloud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Execution confirmation: the caller must type the exact target resource identifier.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationExecuteRequest {

    @NotBlank
    @Size(max = 2048)
    private String confirmResourceId;
}
