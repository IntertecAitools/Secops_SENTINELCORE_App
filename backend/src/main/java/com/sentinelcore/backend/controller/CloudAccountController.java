package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.dto.cloud.*;
import com.sentinelcore.backend.service.CloudAccountService;
import com.sentinelcore.backend.service.CloudScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cloud")
@RequiredArgsConstructor
public class CloudAccountController {

    private final CloudAccountService cloudAccountService;
    private final CloudScanService cloudScanService;

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_VIEW')")
    @GetMapping("/capabilities")
    public ResponseEntity<ApiResponse<CloudCapabilitiesDTO>> getCapabilities() {
        return ResponseEntity.ok(ApiResponse.success("Cloud capabilities retrieved", cloudAccountService.capabilities()));
    }

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_VIEW')")
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<CloudAccountDTO>>> getAccounts() {
        return ResponseEntity.ok(ApiResponse.success("Cloud accounts retrieved", cloudAccountService.listAccounts()));
    }

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_VIEW')")
    @GetMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<CloudAccountDTO>> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Cloud account retrieved", cloudAccountService.getAccount(id)));
    }

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_MANAGE')")
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<CloudAccountDTO>> createAccount(@Valid @RequestBody CloudAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cloud account registered", cloudAccountService.createAccount(request)));
    }

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_MANAGE')")
    @PutMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<CloudAccountDTO>> updateAccount(@PathVariable Long id,
                                                                      @Valid @RequestBody CloudAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cloud account updated", cloudAccountService.updateAccount(id, request)));
    }

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_MANAGE')")
    @PostMapping("/accounts/{id}/verify")
    public ResponseEntity<ApiResponse<CloudAccountDTO>> verifyAccount(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Connection check completed", cloudAccountService.verifyConnection(id)));
    }

    @PreAuthorize("hasAuthority('CLOUD_SCAN_RUN')")
    @PostMapping("/accounts/{id}/scans")
    public ResponseEntity<ApiResponse<CloudScanDTO>> runScan(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Scan completed", cloudScanService.runScan(id)));
    }

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_VIEW')")
    @GetMapping("/accounts/{id}/scans")
    public ResponseEntity<ApiResponse<Page<CloudScanDTO>>> getScans(@PathVariable Long id,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Scans retrieved",
                cloudScanService.listScans(id, PageRequest.of(page, Math.min(size, 100)))));
    }

    @PreAuthorize("hasAuthority('CLOUD_ACCOUNT_VIEW')")
    @GetMapping("/accounts/{id}/resources")
    public ResponseEntity<ApiResponse<Page<CloudResourceDTO>>> getResources(@PathVariable Long id,
                                                                            @RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved",
                cloudScanService.listResources(id, PageRequest.of(page, Math.min(size, 200),
                        Sort.by("resourceType").ascending().and(Sort.by("name").ascending())))));
    }
}
