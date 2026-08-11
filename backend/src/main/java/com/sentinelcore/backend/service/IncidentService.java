package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.IncidentDTO;
import com.sentinelcore.backend.entity.Alert;
import com.sentinelcore.backend.entity.Incident;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.AlertRepository;
import com.sentinelcore.backend.repository.IncidentRepository;
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
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final AlertRepository alertRepository;

    public Page<IncidentDTO> getIncidents(String search, String severity, String status, Pageable pageable) {
        log.info("Fetching incidents with search: {}, severity: {}, status: {}", search, severity, status);

        Specification<Incident> spec = Specification.where(null);

        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("resolutionNotes")), "%" + search.toLowerCase() + "%")
            ));
        }

        if (StringUtils.hasText(severity)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("severity")), severity.toUpperCase()));
        }

        if (StringUtils.hasText(status)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.toUpperCase()));
        }

        return incidentRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    public IncidentDTO getIncidentById(Long id) {
        log.info("Fetching incident by id: {}", id);
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));
        return convertToDTO(incident);
    }

    public IncidentDTO createIncident(IncidentDTO dto) {
        log.info("Creating incident: {}", dto.getTitle());
        Incident incident = convertToEntity(dto);
        Incident saved = incidentRepository.save(incident);
        return convertToDTO(saved);
    }

    public IncidentDTO updateIncident(Long id, IncidentDTO dto) {
        log.info("Updating incident with id: {}", id);
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + id));

        incident.setTitle(dto.getTitle());
        incident.setDescription(dto.getDescription());
        incident.setSeverity(dto.getSeverity());
        incident.setStatus(dto.getStatus());
        incident.setResolutionNotes(dto.getResolutionNotes());

        if (dto.getAssignedToId() != null) {
            User user = userRepository.findById(dto.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getAssignedToId()));
            incident.setAssignedTo(user);
        } else {
            incident.setAssignedTo(null);
        }

        if (dto.getAlertId() != null) {
            Alert alert = alertRepository.findById(dto.getAlertId())
                    .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + dto.getAlertId()));
            incident.setAlert(alert);
        } else {
            incident.setAlert(null);
        }

        Incident updated = incidentRepository.save(incident);
        return convertToDTO(updated);
    }

    public void deleteIncident(Long id) {
        log.info("Deleting incident with id: {}", id);
        if (!incidentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Incident not found with id: " + id);
        }
        incidentRepository.deleteById(id);
    }

    public IncidentDTO convertToDTO(Incident incident) {
        return IncidentDTO.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .resolutionNotes(incident.getResolutionNotes())
                .assignedToId(incident.getAssignedTo() != null ? incident.getAssignedTo().getId() : null)
                .assignedToName(incident.getAssignedTo() != null ? incident.getAssignedTo().getFullName() : null)
                .alertId(incident.getAlert() != null ? incident.getAlert().getId() : null)
                .alertTitle(incident.getAlert() != null ? incident.getAlert().getTitle() : null)
                .createdAt(incident.getCreatedAt())
                .build();
    }

    private Incident convertToEntity(IncidentDTO dto) {
        Incident incident = Incident.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .severity(dto.getSeverity())
                .status(dto.getStatus())
                .resolutionNotes(dto.getResolutionNotes())
                .build();

        if (dto.getAssignedToId() != null) {
            User user = userRepository.findById(dto.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getAssignedToId()));
            incident.setAssignedTo(user);
        }

        if (dto.getAlertId() != null) {
            Alert alert = alertRepository.findById(dto.getAlertId())
                    .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + dto.getAlertId()));
            incident.setAlert(alert);
        }
        return incident;
    }
}
