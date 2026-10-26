package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.DashboardStatsDTO;
import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PreAuthorize("hasAnyAuthority('ASSET_VIEW', 'ALERT_VIEW', 'INCIDENT_VIEW', 'VULNERABILITY_VIEW', 'RISK_VIEW', 'AUDIT_LOG_VIEW')")
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsDTO> getDashboardStats() {

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        return ApiResponse.success(
                "Dashboard statistics retrieved successfully",
                stats
        );
    }
}