package com.sentinelcore.backend.repository;

import com.sentinelcore.backend.entity.ThreatIntel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ThreatIntelRepository extends JpaRepository<ThreatIntel, Long>, JpaSpecificationExecutor<ThreatIntel> {

}
