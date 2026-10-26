package com.sentinelcore.backend.repository;

import com.sentinelcore.backend.entity.CloudScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CloudScanRepository extends JpaRepository<CloudScan, Long> {

    boolean existsByCloudAccountIdAndStatusAndStartedAtAfter(Long cloudAccountId, CloudScan.Status status,
                                                              LocalDateTime startedAfter);

    Page<CloudScan> findByTenantKeyAndCloudAccountIdOrderByStartedAtDesc(String tenantKey, Long cloudAccountId,
                                                                         Pageable pageable);

    Optional<CloudScan> findFirstByCloudAccountIdOrderByStartedAtDesc(Long cloudAccountId);
}
