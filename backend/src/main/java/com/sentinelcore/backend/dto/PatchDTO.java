package com.sentinelcore.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchDTO {

    private Long id;

    private String title;

    private String description;

    private String version;

    private String status;

    private String targetOs;

    private Long assetId;

    private String assetName;

    private LocalDateTime releaseDate;

    private LocalDateTime appliedAt;

    private LocalDateTime createdAt;
}
