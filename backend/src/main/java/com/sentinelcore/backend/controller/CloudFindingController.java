package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.dto.cloud.CloudFindingDTO;
import com.sentinelcore.backend.service.CloudScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cloud/findings")
@RequiredArgsConstructor
public class CloudFindingController {

    private final CloudScanService cloudScanService;

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_VIEW')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CloudFindingDTO>>> getFindings(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Sort is fixed server-side; client-controlled sort properties are not accepted here
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("lastObservedAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Findings retrieved",
                cloudScanService.listFindings(accountId, status, severity, search, pageable)));
    }

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CloudFindingDTO>> getFinding(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Finding retrieved", cloudScanService.getFinding(id)));
    }

    @PreAuthorize("hasAuthority('CLOUD_SCAN_RUN')")
    @PostMapping("/{id}/recheck")
    public ResponseEntity<ApiResponse<CloudFindingDTO>> recheckFinding(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Finding re-evaluated", cloudScanService.recheckFinding(id)));
    }
}
