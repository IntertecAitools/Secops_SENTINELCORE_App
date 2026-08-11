package com.sentinelcore.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false)
    private Integer riskScore; // 1 to 25 or 1 to 100

    @Column(nullable = false)
    private String likelihood; // HIGH, MEDIUM, LOW

    @Column(nullable = false)
    private String impact; // HIGH, MEDIUM, LOW

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assessed_by_id")
    private User assessedBy;

    @Column(nullable = false)
    private LocalDateTime assessmentDate;

    @PrePersist
    public void prePersist() {
        this.assessmentDate = LocalDateTime.now();
    }
}
