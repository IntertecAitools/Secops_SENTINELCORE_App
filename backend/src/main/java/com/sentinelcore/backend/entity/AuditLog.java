package com.sentinelcore.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Append-only security audit record. Hibernate never issues UPDATEs for this entity and the
 * application exposes no update or delete path. Database-level protection (REVOKE UPDATE, DELETE)
 * is documented in docs/security-audit.md.
 */
@Entity
@Immutable
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String action; // e.g., USER_LOGIN, ASSET_CREATE, INCIDENT_RESOLVE

    @Column(nullable = false, updatable = false)
    private String username; // user who performed action

    @Column(columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(updatable = false)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}
