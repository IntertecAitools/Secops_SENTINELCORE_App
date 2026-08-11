package com.sentinelcore.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessmentDTO {

    private Long id;

    private Long assetId;

    private String assetName;

    private Integer riskScore;

    private String likelihood;

    private String impact;

    private String description;

    private Long assessedById;

    private String assessedByName;

    private LocalDateTime assessmentDate;
}
