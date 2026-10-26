package com.sentinelcore.backend.dto.cloud;

import com.sentinelcore.backend.cloud.RemediationActionType;
import com.sentinelcore.backend.entity.RemediationRequest;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationDTO {

    private Long id;

    private Long cloudAccountId;

    private String cloudAccountName;

    private Long findingId;

    private String findingTitle;

    private RemediationActionType actionType;

    private boolean highImpact;

    private String resourceType;

    private String resourceId;

    private String region;

    private String requestHash;

    private RemediationRequest.Status status;

    private String requestedBy;

    private LocalDateTime requestedAt;

    private String justification;

    private LocalDateTime validatedAt;

    private String validationNotes;

    private String decidedBy;

    private LocalDateTime decidedAt;

    private String decisionComment;

    private String cancelledBy;

    private LocalDateTime cancelledAt;

    private String executedBy;

    private LocalDateTime executionStartedAt;

    private LocalDateTime executionCompletedAt;

    private String executionMessage;

    private String providerRequestId;

    private Integer executionAttempts;

    private LocalDateTime verifiedAt;

    private String verificationResult;
}
