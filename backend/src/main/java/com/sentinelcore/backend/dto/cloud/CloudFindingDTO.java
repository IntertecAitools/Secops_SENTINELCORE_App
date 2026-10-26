package com.sentinelcore.backend.dto.cloud;

import com.sentinelcore.backend.cloud.RemediationActionType;
import com.sentinelcore.backend.entity.CloudFinding;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudFindingDTO {

    private Long id;

    private Long cloudAccountId;

    private String cloudAccountName;

    private String findingType;

    private String severity;

    private CloudFinding.Status status;

    private String resourceType;

    private String resourceId;

    private String region;

    private String title;

    private String observedFact;

    private String inferredRisk;

    private Map<String, String> evidence;

    private String source;

    private String remediationGuidance;

    private RemediationActionType supportedRemediation;

    private LocalDateTime firstDetectedAt;

    private LocalDateTime lastObservedAt;

    private LocalDateTime resolvedAt;

    private String resolutionReason;

    private Integer reopenCount;
}
