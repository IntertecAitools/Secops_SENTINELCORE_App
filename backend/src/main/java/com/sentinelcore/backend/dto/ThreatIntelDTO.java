package com.sentinelcore.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreatIntelDTO {

    private Long id;

    private String indicatorType;

    private String value;

    private String threatType;

    private String confidenceLevel;

    private String description;

    private String source;

    private LocalDateTime lastObserved;

    private LocalDateTime createdAt;
}
