package com.sentinelcore.backend.repository;

import com.sentinelcore.backend.entity.CloudFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CloudFindingRepository extends JpaRepository<CloudFinding, Long>, JpaSpecificationExecutor<CloudFinding> {

    Optional<CloudFinding> findByIdAndTenantKey(Long id, String tenantKey);

    List<CloudFinding> findByCloudAccountId(Long cloudAccountId);

    List<CloudFinding> findByCloudAccountIdAndResourceIdAndStatus(Long cloudAccountId, String resourceId,
                                                                  CloudFinding.Status status);
}
