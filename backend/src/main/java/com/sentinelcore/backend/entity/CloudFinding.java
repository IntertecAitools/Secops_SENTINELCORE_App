package com.sentinelcore.backend.entity;

import com.sentinelcore.backend.cloud.RemediationActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A cloud configuration finding. Rows are never deleted: a finding is resolved only when a complete
 * scan (or a post-remediation verification) observes that the condition no longer holds, and it is
 * reopened if the condition reappears. The fingerprint makes repeated scans idempotent.
 */
@Entity
@Table(name = "cloud_findings",
        uniqueConstraints = @UniqueConstraint(name = "uk_cloud_finding_account_fingerprint",
                columnNames = {"cloud_account_id", "fingerprint"}),
        indexes = {
                @Index(name = "idx_cloud_findings_tenant_status", columnList = "tenant_key, status"),
                @Index(name = "idx_cloud_findings_account_status", columnList = "cloud_account_id, status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudFinding {

    public enum Status { OPEN, RESOLVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, length = 64, updatable = false)
    private String tenantKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_account_id", nullable = false, updatable = false)
    private CloudAccount cloudAccount;

    @Column(nullable = false, length = 64, updatable = false)
    private String fingerprint;

    @Column(nullable = false, length = 64, updatable = false)
    private String findingType;

    @Column(nullable = false, length = 16)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW (same vocabulary as vulnerabilities/alerts)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(nullable = false, length = 64, updatable = false)
    private String resourceType;

    @Column(nullable = false, length = 2048, updatable = false)
    private String resourceId;

    @Column(length = 64)
    private String region;

    @Column(nullable = false, length = 128)
    private String scope;

    @Column(nullable = false, length = 512)
    private String title;

    /** What the provider API returned. */
    @Column(columnDefinition = "TEXT")
    private String observedFact;

    /** The platform's interpretation of the observed fact. */
    @Column(columnDefinition = "TEXT")
    private String inferredRisk;

    /** Key/value evidence captured at the last observation, serialized as JSON. */
    @Column(columnDefinition = "TEXT")
    private String evidenceJson;

    /** Provider API that produced the observation. */
    @Column(length = 255)
    private String source;

    @Column(columnDefinition = "TEXT")
    private String remediationGuidance;

    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private RemediationActionType supportedRemediation;

    @Column(nullable = false, updatable = false)
    private LocalDateTime firstDetectedAt;

    @Column(nullable = false)
    private LocalDateTime lastObservedAt;

    private Long lastScanId;

    private LocalDateTime resolvedAt;

    @Column(length = 512)
    private String resolutionReason;

    @Column(nullable = false)
    @Builder.Default
    private Integer reopenCount = 0;
}
