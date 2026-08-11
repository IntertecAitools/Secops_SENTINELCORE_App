package com.sentinelcore.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDTO {

    private Long id;

    private String title;

    private String description;

    private String severity;

    private String status;

    private String sourceIp;

    private String category;

    private Long assetId;

    private String assetName;

    private LocalDateTime createdAt;
}
