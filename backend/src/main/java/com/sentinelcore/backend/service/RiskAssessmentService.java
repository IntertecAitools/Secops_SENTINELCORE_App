package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.RiskAssessmentDTO;
import com.sentinelcore.backend.entity.Asset;
import com.sentinelcore.backend.entity.RiskAssessment;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.AssetRepository;
import com.sentinelcore.backend.repository.RiskAssessmentRepository;
import com.sentinelcore.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    public Page<RiskAssessmentDTO> getRiskAssessments(String search, Pageable pageable) {
        log.info("Fetching risk assessments with search: {}", search);

        Specification<RiskAssessment> spec = Specification.where(null);

        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("asset").get("assetName")), "%" + search.toLowerCase() + "%")
            ));
        }

        return riskAssessmentRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    public RiskAssessmentDTO getRiskAssessmentById(Long id) {
        log.info("Fetching risk assessment by id: {}", id);
        RiskAssessment assessment = riskAssessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RiskAssessment not found with id: " + id));
        return convertToDTO(assessment);
    }

    public RiskAssessmentDTO createRiskAssessment(RiskAssessmentDTO dto) {
        log.info("Creating risk assessment for asset ID: {}", dto.getAssetId());
        RiskAssessment assessment = convertToEntity(dto);
        // Calculate risk score: 1 to 25 matrix (Likelihood [1..5] * Impact [1..5])
        int lVal = getFactorValue(dto.getLikelihood());
        int iVal = getFactorValue(dto.getImpact());
        assessment.setRiskScore(lVal * iVal * 4); // Scaled to 4-100% risk representation

        RiskAssessment saved = riskAssessmentRepository.save(assessment);
        return convertToDTO(saved);
    }

    public RiskAssessmentDTO updateRiskAssessment(Long id, RiskAssessmentDTO dto) {
        log.info("Updating risk assessment with id: {}", id);
        RiskAssessment assessment = riskAssessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RiskAssessment not found with id: " + id));

        assessment.setLikelihood(dto.getLikelihood());
        assessment.setImpact(dto.getImpact());
        assessment.setDescription(dto.getDescription());

        int lVal = getFactorValue(dto.getLikelihood());
        int iVal = getFactorValue(dto.getImpact());
        assessment.setRiskScore(lVal * iVal * 4);

        if (dto.getAssetId() != null) {
            Asset asset = assetRepository.findById(dto.getAssetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + dto.getAssetId()));
            assessment.setAsset(asset);
        }

        if (dto.getAssessedById() != null) {
            User user = userRepository.findById(dto.getAssessedById())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getAssessedById()));
            assessment.setAssessedBy(user);
        }

        RiskAssessment updated = riskAssessmentRepository.save(assessment);
        return convertToDTO(updated);
    }

    public void deleteRiskAssessment(Long id) {
        log.info("Deleting risk assessment with id: {}", id);
        if (!riskAssessmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("RiskAssessment not found with id: " + id);
        }
        riskAssessmentRepository.deleteById(id);
    }

    private int getFactorValue(String value) {
        if (value == null) return 1;
        switch (value.toUpperCase()) {
            case "HIGH":
            case "CRITICAL":
                return 5;
            case "MEDIUM":
                return 3;
            case "LOW":
            default:
                return 1;
        }
    }

    private RiskAssessmentDTO convertToDTO(RiskAssessment entity) {
        return RiskAssessmentDTO.builder()
                .id(entity.getId())
                .assetId(entity.getAsset().getId())
                .assetName(entity.getAsset().getAssetName())
                .riskScore(entity.getRiskScore())
                .likelihood(entity.getLikelihood())
                .impact(entity.getImpact())
                .description(entity.getDescription())
                .assessedById(entity.getAssessedBy() != null ? entity.getAssessedBy().getId() : null)
                .assessedByName(entity.getAssessedBy() != null ? entity.getAssessedBy().getFullName() : null)
                .assessmentDate(entity.getAssessmentDate())
                .build();
    }

    private RiskAssessment convertToEntity(RiskAssessmentDTO dto) {
        Asset asset = assetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + dto.getAssetId()));

        User user = null;
        if (dto.getAssessedById() != null) {
            user = userRepository.findById(dto.getAssessedById()).orElse(null);
        }

        return RiskAssessment.builder()
                .id(dto.getId())
                .asset(asset)
                .likelihood(dto.getLikelihood())
                .impact(dto.getImpact())
                .description(dto.getDescription())
                .assessedBy(user)
                .build();
    }
}
