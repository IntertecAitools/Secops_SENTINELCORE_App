package com.sentinelcore.backend.repository;

import com.sentinelcore.backend.entity.Patch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PatchRepository extends JpaRepository<Patch, Long>, JpaSpecificationExecutor<Patch> {

}
