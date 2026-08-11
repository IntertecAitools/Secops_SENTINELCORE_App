package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.ThreatIntelDTO;
import com.sentinelcore.backend.entity.ThreatIntel;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.ThreatIntelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThreatIntelService {

    private final ThreatIntelRepository threatIntelRepository;

    public Page<ThreatIntelDTO> getThreatIntels(String search, String indicatorType, String confidenceLevel, Pageable pageable) {
        log.info("Fetching threat intelligence with search: {}, type: {}, confidence: {}", search, indicatorType, confidenceLevel);

        Specification<ThreatIntel> spec = Specification.where(null);

        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("value")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("threatType")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("source")), "%" + search.toLowerCase() + "%")
            ));
        }

        if (StringUtils.hasText(indicatorType)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("indicatorType")), indicatorType.toUpperCase()));
        }

        if (StringUtils.hasText(confidenceLevel)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("confidenceLevel")), confidenceLevel.toUpperCase()));
        }

        return threatIntelRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    public ThreatIntelDTO getThreatIntelById(Long id) {
        log.info("Fetching threat intel by id: {}", id);
        ThreatIntel entity = threatIntelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ThreatIntel not found with id: " + id));
        return convertToDTO(entity);
    }

    public ThreatIntelDTO createThreatIntel(ThreatIntelDTO dto) {
        log.info("Creating threat intel entry for: {}", dto.getValue());
        ThreatIntel entity = convertToEntity(dto);
        ThreatIntel saved = threatIntelRepository.save(entity);
        return convertToDTO(saved);
    }

    public ThreatIntelDTO updateThreatIntel(Long id, ThreatIntelDTO dto) {
        log.info("Updating threat intel entry with id: {}", id);
        ThreatIntel entity = threatIntelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ThreatIntel not found with id: " + id));

        entity.setIndicatorType(dto.getIndicatorType());
        entity.setValue(dto.getValue());
        entity.setThreatType(dto.getThreatType());
        entity.setConfidenceLevel(dto.getConfidenceLevel());
        entity.setDescription(dto.getDescription());
        entity.setSource(dto.getSource());
        if (dto.getLastObserved() != null) {
            entity.setLastObserved(dto.getLastObserved());
        }

        ThreatIntel updated = threatIntelRepository.save(entity);
        return convertToDTO(updated);
    }

    public void deleteThreatIntel(Long id) {
        log.info("Deleting threat intel with id: {}", id);
        if (!threatIntelRepository.existsById(id)) {
            throw new ResourceNotFoundException("ThreatIntel not found with id: " + id);
        }
        threatIntelRepository.deleteById(id);
    }

    private ThreatIntelDTO convertToDTO(ThreatIntel entity) {
        return ThreatIntelDTO.builder()
                .id(entity.getId())
                .indicatorType(entity.getIndicatorType())
                .value(entity.getValue())
                .threatType(entity.getThreatType())
                .confidenceLevel(entity.getConfidenceLevel())
                .description(entity.getDescription())
                .source(entity.getSource())
                .lastObserved(entity.getLastObserved())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ThreatIntel convertToEntity(ThreatIntelDTO dto) {
        return ThreatIntel.builder()
                .id(dto.getId())
                .indicatorType(dto.getIndicatorType())
                .value(dto.getValue())
                .threatType(dto.getThreatType())
                .confidenceLevel(dto.getConfidenceLevel())
                .description(dto.getDescription())
                .source(dto.getSource())
                .lastObserved(dto.getLastObserved() != null ? dto.getLastObserved() : LocalDateTime.now())
                .build();
    }
}
