package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.IncidentDTO;
import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PreAuthorize("hasAuthority('INCIDENT_VIEW')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<IncidentDTO>>> getIncidents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<IncidentDTO> incidents = incidentService.getIncidents(search, severity, status, pageable);

        return ResponseEntity.ok(ApiResponse.success("Incidents retrieved successfully", incidents));
    }

    @PreAuthorize("hasAuthority('INCIDENT_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentDTO>> getIncidentById(@PathVariable Long id) {
        IncidentDTO incident = incidentService.getIncidentById(id);
        return ResponseEntity.ok(ApiResponse.success("Incident retrieved successfully", incident));
    }

    @PreAuthorize("hasAuthority('INCIDENT_MANAGE')")
    @PostMapping
    public ResponseEntity<ApiResponse<IncidentDTO>> createIncident(@RequestBody IncidentDTO dto) {
        IncidentDTO saved = incidentService.createIncident(dto);
        return ResponseEntity.ok(ApiResponse.success("Incident created successfully", saved));
    }

    @PreAuthorize("hasAuthority('INCIDENT_MANAGE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentDTO>> updateIncident(@PathVariable Long id, @RequestBody IncidentDTO dto) {
        IncidentDTO updated = incidentService.updateIncident(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Incident updated successfully", updated));
    }

    @PreAuthorize("hasAuthority('INCIDENT_MANAGE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteIncident(@PathVariable Long id) {
        incidentService.deleteIncident(id);
        return ResponseEntity.ok(ApiResponse.success("Incident deleted successfully", null));
    }
}
