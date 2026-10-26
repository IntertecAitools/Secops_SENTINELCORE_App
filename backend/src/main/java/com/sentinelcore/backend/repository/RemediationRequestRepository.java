package com.sentinelcore.backend.repository;

import com.sentinelcore.backend.entity.RemediationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface RemediationRequestRepository extends JpaRepository<RemediationRequest, Long> {

    Optional<RemediationRequest> findByIdAndTenantKey(Long id, String tenantKey);

    Optional<RemediationRequest> findByTenantKeyAndIdempotencyKey(String tenantKey, String idempotencyKey);

    Page<RemediationRequest> findByTenantKeyOrderByRequestedAtDesc(String tenantKey, Pageable pageable);

    Page<RemediationRequest> findByTenantKeyAndStatusOrderByRequestedAtDesc(String tenantKey,
                                                                           RemediationRequest.Status status,
                                                                           Pageable pageable);

    boolean existsByFindingIdAndStatusIn(Long findingId, Collection<RemediationRequest.Status> statuses);
}
