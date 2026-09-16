package com.sentinelcore.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {
    private String reply;
    private String category;
    private String actionUrl;
    private String actionText;
    private LocalDateTime timestamp;
}
