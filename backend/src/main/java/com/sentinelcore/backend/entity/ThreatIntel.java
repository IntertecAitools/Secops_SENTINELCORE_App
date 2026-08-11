package com.sentinelcore.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "threat_intel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreatIntel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String indicatorType; // IP, DOMAIN, FILE_HASH, URL

    @Column(nullable = false)
    private String value; // e.g., 192.168.1.50, malicious-domain.com

    @Column(nullable = false)
    private String threatType; // MALWARE, PHISHING, BOTNET, APT

    @Column(nullable = false)
    private String confidenceLevel; // HIGH, MEDIUM, LOW

    @Column(columnDefinition = "TEXT")
    private String description;

    private String source; // OSINT, CrowdStrike, AlienVault, Internal

    private LocalDateTime lastObserved;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.lastObserved == null) {
            this.lastObserved = LocalDateTime.now();
        }
    }
}
