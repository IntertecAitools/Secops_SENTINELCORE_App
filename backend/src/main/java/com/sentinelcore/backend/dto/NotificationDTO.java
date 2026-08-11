package com.sentinelcore.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {

    private Long id;

    private String message;

    private Boolean isRead;

    private String severity;

    private LocalDateTime createdAt;
}
