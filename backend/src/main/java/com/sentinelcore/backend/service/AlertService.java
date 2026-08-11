package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.AlertDTO;
import com.sentinelcore.backend.entity.Alert;
import com.sentinelcore.backend.entity.Asset;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.AlertRepository;
import com.sentinelcore.backend.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final AssetRepository assetRepository;

    public Page<AlertDTO> getAlerts(String search, String severity, String status, Pageable pageable) {
        log.info("Fetching alerts with search: {}, severity: {}, status: {}", search, severity, status);

        Specification<Alert> spec = Specification.where(null);

        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("category")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("sourceIp")), "%" + search.toLowerCase() + "%")
            ));
        }

        if (StringUtils.hasText(severity)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("severity")), severity.toUpperCase()));
        }

        if (StringUtils.hasText(status)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.toUpperCase()));
        }

        return alertRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    public AlertDTO getAlertById(Long id) {
        log.info("Fetching alert by id: {}", id);
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));
        return convertToDTO(alert);
    }

    public AlertDTO createAlert(AlertDTO dto) {
        log.info("Creating alert: {}", dto.getTitle());
        Alert alert = convertToEntity(dto);
        Alert saved = alertRepository.save(alert);
        return convertToDTO(saved);
    }

    public AlertDTO updateAlert(Long id, AlertDTO dto) {
        log.info("Updating alert with id: {}", id);
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));

        alert.setTitle(dto.getTitle());
        alert.setDescription(dto.getDescription());
        alert.setSeverity(dto.getSeverity());
        alert.setStatus(dto.getStatus());
        alert.setSourceIp(dto.getSourceIp());
        alert.setCategory(dto.getCategory());

        if (dto.getAssetId() != null) {
            Asset asset = assetRepository.findById(dto.getAssetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + dto.getAssetId()));
            alert.setAsset(asset);
        } else {
            alert.setAsset(null);
        }

        Alert updated = alertRepository.save(alert);
        return convertToDTO(updated);
    }

    public void deleteAlert(Long id) {
        log.info("Deleting alert with id: {}", id);
        if (!alertRepository.existsById(id)) {
            throw new ResourceNotFoundException("Alert not found with id: " + id);
        }
        alertRepository.deleteById(id);
    }

    public AlertDTO convertToDTO(Alert alert) {
        return AlertDTO.builder()
                .id(alert.getId())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .sourceIp(alert.getSourceIp())
                .category(alert.getCategory())
                .assetId(alert.getAsset() != null ? alert.getAsset().getId() : null)
                .assetName(alert.getAsset() != null ? alert.getAsset().getAssetName() : null)
                .createdAt(alert.getCreatedAt())
                .build();
    }

    private Alert convertToEntity(AlertDTO dto) {
        Alert alert = Alert.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .severity(dto.getSeverity())
                .status(dto.getStatus())
                .sourceIp(dto.getSourceIp())
                .category(dto.getCategory())
                .build();

        if (dto.getAssetId() != null) {
            Asset asset = assetRepository.findById(dto.getAssetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + dto.getAssetId()));
            alert.setAsset(asset);
        }
        return alert;
    }
}
