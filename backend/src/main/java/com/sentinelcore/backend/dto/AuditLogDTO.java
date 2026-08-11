package com.sentinelcore.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {

    private Long id;

    private String action;

    private String username;

    private String details;

    private String ipAddress;

    private LocalDateTime timestamp;
}
