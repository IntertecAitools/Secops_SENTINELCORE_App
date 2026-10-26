package com.sentinelcore.backend.entity;

import com.sentinelcore.backend.cloud.RemediationActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A controlled remediation workflow instance.
 * <pre>
 * PROPOSED → VALIDATED → PENDING_APPROVAL → APPROVED → EXECUTING → SUCCEEDED → VERIFIED
 *                                 ↘ REJECTED      ↘ CANCELLED     ↘ FAILED      ↘ VERIFICATION_FAILED
 *                                                                 ↘ OUTCOME_UNKNOWN (must be verified)
 * </pre>
 * The action, target and parameters are immutable after creation and covered by {@code requestHash};
 * an approval records the hash it was granted for, so a modified request cannot reuse it.
 * {@code version} provides optimistic locking so only one caller can move APPROVED → EXECUTING.
 */
@Entity
@Table(name = "remediation_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_remediation_tenant_idempotency",
                columnNames = {"tenant_key", "idempotency_key"}),
        indexes = {
                @Index(name = "idx_remediation_tenant_status", columnList = "tenant_key, status"),
                @Index(name = "idx_remediation_finding", columnList = "cloud_finding_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationRequest {

    public enum Status {
        PROPOSED, VALIDATED, PENDING_APPROVAL, APPROVED, REJECTED, CANCELLED,
        EXECUTING, SUCCEEDED, FAILED, OUTCOME_UNKNOWN, VERIFIED, VERIFICATION_FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "tenant_key", nullable = false, length = 64, updatable = false)
    private String tenantKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_account_id", nullable = false, updatable = false)
    private CloudAccount cloudAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_finding_id", nullable = false, updatable = false)
    private CloudFinding finding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64, updatable = false)
    private RemediationActionType actionType;

    @Column(nullable = false, length = 64, updatable = false)
    private String resourceType;

    @Column(nullable = false, length = 2048, updatable = false)
    private String resourceId;

    @Column(length = 64, updatable = false)
    private String region;

    /** Validated action parameters as canonical JSON ("{}" when the action takes none). */
    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String parametersJson;

    @Column(nullable = false, length = 64, updatable = false)
    private String requestHash;

    @Column(name = "idempotency_key", nullable = false, length = 128, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(nullable = false, updatable = false)
    private String requestedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(length = 1000, updatable = false)
    private String justification;

    private LocalDateTime validatedAt;

    @Column(length = 1000)
    private String validationNotes;

    // Approval / rejection decision
    private String decidedBy;

    private LocalDateTime decidedAt;

    @Column(length = 1000)
    private String decisionComment;

    /** Hash of the request the approval was granted for. */
    @Column(length = 64)
    private String approvalHash;

    private String cancelledBy;

    private LocalDateTime cancelledAt;

    // Execution
    private String executedBy;

    private LocalDateTime executionStartedAt;

    private LocalDateTime executionCompletedAt;

    @Column(length = 2048)
    private String executionMessage;

    /** Provider request id for correlation with provider-side logs (e.g. CloudTrail). */
    @Column(length = 255)
    private String providerRequestId;

    @Column(nullable = false)
    @Builder.Default
    private Integer executionAttempts = 0;

    // Verification
    private LocalDateTime verifiedAt;

    @Column(length = 2048)
    private String verificationResult;
}
