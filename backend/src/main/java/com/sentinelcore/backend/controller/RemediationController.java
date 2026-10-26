package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.dto.cloud.*;
import com.sentinelcore.backend.service.RemediationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cloud/remediations")
@RequiredArgsConstructor
public class RemediationController {

    private final RemediationService remediationService;

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_VIEW')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RemediationDTO>>> getRemediations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Remediations retrieved",
                remediationService.list(status, PageRequest.of(page, Math.min(size, 100)))));
    }

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RemediationDTO>> getRemediation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Remediation retrieved", remediationService.getRemediation(id)));
    }

    @PreAuthorize("hasAuthority('REMEDIATION_REQUEST')")
    @PostMapping
    public ResponseEntity<ApiResponse<RemediationDTO>> createRemediation(
            @Valid @RequestBody RemediationCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(ApiResponse.success("Remediation requested",
                remediationService.create(request, idempotencyKey)));
    }

    @PreAuthorize("hasAnyAuthority('REMEDIATION_REQUEST', 'REMEDIATION_APPROVE', 'REMEDIATION_EXECUTE')")
    @GetMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<RemediationPreviewDTO>> previewRemediation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Remediation preview", remediationService.preview(id)));
    }

    @PreAuthorize("hasAuthority('REMEDIATION_APPROVE')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<RemediationDTO>> approveRemediation(
            @PathVariable Long id, @Valid @RequestBody RemediationDecisionRequest decision) {
        return ResponseEntity.ok(ApiResponse.success("Remediation approved", remediationService.approve(id, decision)));
    }

    @PreAuthorize("hasAuthority('REMEDIATION_APPROVE')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<RemediationDTO>> rejectRemediation(
            @PathVariable Long id, @Valid @RequestBody RemediationDecisionRequest decision) {
        return ResponseEntity.ok(ApiResponse.success("Remediation rejected", remediationService.reject(id, decision)));
    }

    @PreAuthorize("hasAnyAuthority('REMEDIATION_REQUEST', 'REMEDIATION_APPROVE')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RemediationDTO>> cancelRemediation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Remediation cancelled", remediationService.cancel(id)));
    }

    @PreAuthorize("hasAuthority('REMEDIATION_EXECUTE')")
    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<RemediationDTO>> executeRemediation(
            @PathVariable Long id, @Valid @RequestBody RemediationExecuteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Remediation execution finished",
                remediationService.execute(id, request)));
    }

    @PreAuthorize("hasAnyAuthority('REMEDIATION_EXECUTE', 'REMEDIATION_APPROVE')")
    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<RemediationDTO>> verifyRemediation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Verification completed", remediationService.verify(id)));
    }
}
