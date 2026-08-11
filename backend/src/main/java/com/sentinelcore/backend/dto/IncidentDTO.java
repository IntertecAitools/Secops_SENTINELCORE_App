package com.sentinelcore.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentDTO {

    private Long id;

    private String title;

    private String description;

    private String severity;

    private String status;

    private String resolutionNotes;

    private Long assignedToId;

    private String assignedToName;

    private Long alertId;

    private String alertTitle;

    private LocalDateTime createdAt;
}
