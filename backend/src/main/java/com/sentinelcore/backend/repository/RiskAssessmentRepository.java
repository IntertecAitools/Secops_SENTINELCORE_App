package com.sentinelcore.backend.repository;

import com.sentinelcore.backend.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long>, JpaSpecificationExecutor<RiskAssessment> {

}
