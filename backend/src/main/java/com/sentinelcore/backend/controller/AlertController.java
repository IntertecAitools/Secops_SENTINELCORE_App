package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.AlertDTO;
import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AlertDTO>>> getAlerts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AlertDTO> alerts = alertService.getAlerts(search, severity, status, pageable);

        return ResponseEntity.ok(ApiResponse.success("Alerts retrieved successfully", alerts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AlertDTO>> getAlertById(@PathVariable Long id) {
        AlertDTO alert = alertService.getAlertById(id);
        return ResponseEntity.ok(ApiResponse.success("Alert retrieved successfully", alert));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AlertDTO>> createAlert(@RequestBody AlertDTO dto) {
        AlertDTO saved = alertService.createAlert(dto);
        return ResponseEntity.ok(ApiResponse.success("Alert created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AlertDTO>> updateAlert(@PathVariable Long id, @RequestBody AlertDTO dto) {
        AlertDTO updated = alertService.updateAlert(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Alert updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.ok(ApiResponse.success("Alert deleted successfully", null));
    }
}
