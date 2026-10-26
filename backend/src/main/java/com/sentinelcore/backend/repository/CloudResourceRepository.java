package com.sentinelcore.backend.repository;

import com.sentinelcore.backend.entity.CloudResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CloudResourceRepository extends JpaRepository<CloudResource, Long> {

    List<CloudResource> findByCloudAccountId(Long cloudAccountId);

    Page<CloudResource> findByTenantKeyAndCloudAccountId(String tenantKey, Long cloudAccountId, Pageable pageable);
}
