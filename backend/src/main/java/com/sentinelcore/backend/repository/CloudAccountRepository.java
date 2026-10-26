package com.sentinelcore.backend.repository;

import com.sentinelcore.backend.cloud.CloudProvider;
import com.sentinelcore.backend.entity.CloudAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * All lookups are tenant-scoped; there is intentionally no unscoped finder used by services.
 */
@Repository
public interface CloudAccountRepository extends JpaRepository<CloudAccount, Long> {

    List<CloudAccount> findByTenantKeyOrderByNameAsc(String tenantKey);

    Optional<CloudAccount> findByIdAndTenantKey(Long id, String tenantKey);

    boolean existsByTenantKeyAndProviderAndExternalAccountId(String tenantKey, CloudProvider provider,
                                                             String externalAccountId);
}
