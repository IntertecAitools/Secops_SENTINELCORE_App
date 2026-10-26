package com.sentinelcore.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Cloud resource inventory observed by scans. Kept separate from {@link Asset}, whose model requires
 * a unique IP address that cloud resources such as S3 buckets do not have.
 */
@Entity
@Table(name = "cloud_resources",
        uniqueConstraints = @UniqueConstraint(name = "uk_cloud_resource_account_resource",
                columnNames = {"cloud_account_id", "resource_id"}),
        indexes = @Index(name = "idx_cloud_resources_tenant_account", columnList = "tenant_key, cloud_account_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, length = 64, updatable = false)
    private String tenantKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_account_id", nullable = false, updatable = false)
    private CloudAccount cloudAccount;

    @Column(nullable = false, length = 64)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 2048)
    private String resourceId;

    @Column(length = 64)
    private String region;

    @Column(length = 512)
    private String name;

    @Column(nullable = false, updatable = false)
    private LocalDateTime firstSeenAt;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt;

    private Long lastScanId;
}
