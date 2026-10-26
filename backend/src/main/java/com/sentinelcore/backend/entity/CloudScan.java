package com.sentinelcore.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One assessment run against a cloud account.
 */
@Entity
@Table(name = "cloud_scans",
        indexes = @Index(name = "idx_cloud_scans_account_started", columnList = "cloud_account_id, started_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudScan {

    public enum Status { RUNNING, SUCCEEDED, PARTIAL, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, length = 64, updatable = false)
    private String tenantKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_account_id", nullable = false, updatable = false)
    private CloudAccount cloudAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private String triggeredBy;

    private Integer resourcesObserved;

    private Integer findingsObserved;

    private Integer findingsResolved;

    private Integer findingsReopened;

    /** Comma-separated scopes that completed successfully. */
    @Column(length = 2048)
    private String completedScopes;

    /** Sanitized, user-safe error summary for incomplete scopes. */
    @Column(columnDefinition = "TEXT")
    private String errorSummary;
}
