package com.sentinelcore.backend.dto.cloud;

import com.sentinelcore.backend.entity.CloudScan;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudScanDTO {

    private Long id;

    private Long cloudAccountId;

    private CloudScan.Status status;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String triggeredBy;

    private Integer resourcesObserved;

    private Integer findingsObserved;

    private Integer findingsResolved;

    private Integer findingsReopened;

    private List<String> completedScopes;

    private String errorSummary;
}
