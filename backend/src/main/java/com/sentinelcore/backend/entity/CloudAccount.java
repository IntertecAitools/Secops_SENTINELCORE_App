package com.sentinelcore.backend.entity;

import com.sentinelcore.backend.cloud.CloudModels.AuthMode;
import com.sentinelcore.backend.cloud.CloudProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A registered cloud account. Stores connection metadata only: no access keys, secrets or tokens.
 * AWS access uses the platform's own identity and, optionally, a role assumed via STS.
 */
@Entity
@Table(name = "cloud_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_cloud_account_tenant_provider_external",
                columnNames = {"tenant_key", "provider", "external_account_id"}),
        indexes = @Index(name = "idx_cloud_accounts_tenant", columnList = "tenant_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudAccount {

    public enum ConnectionStatus { NOT_VERIFIED, CONNECTED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, length = 64, updatable = false)
    private String tenantKey;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private CloudProvider provider;

    @Column(name = "external_account_id", nullable = false, length = 64, updatable = false)
    private String externalAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthMode authMode;

    @Column(length = 2048)
    private String roleArn;

    /** STS ExternalId (confused-deputy guard). Not a credential, but never returned to clients. */
    @Column(length = 1224)
    private String externalId;

    /** Comma-separated region codes for regional services. */
    @Column(length = 1024)
    private String regions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConnectionStatus connectionStatus;

    private LocalDateTime lastVerifiedAt;

    @Column(length = 1000)
    private String lastVerificationMessage;

    private LocalDateTime lastScanAt;

    @Column(nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.connectionStatus == null) {
            this.connectionStatus = ConnectionStatus.NOT_VERIFIED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
