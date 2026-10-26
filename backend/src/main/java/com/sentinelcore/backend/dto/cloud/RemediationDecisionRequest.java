package com.sentinelcore.backend.dto.cloud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Approve / reject payload. {@code requestHash} must equal the hash of the request the reviewer
 * inspected, binding the decision to that exact action and target.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationDecisionRequest {

    @NotBlank
    @Pattern(regexp = "^[a-f0-9]{64}$", message = "must be a SHA-256 hex digest")
    private String requestHash;

    @Size(max = 1000)
    private String comment;
}
