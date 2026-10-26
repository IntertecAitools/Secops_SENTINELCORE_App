package com.sentinelcore.backend.dto.cloud;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudResourceDTO {

    private Long id;

    private Long cloudAccountId;

    private String resourceType;

    private String resourceId;

    private String region;

    private String name;

    private LocalDateTime firstSeenAt;

    private LocalDateTime lastSeenAt;
}
