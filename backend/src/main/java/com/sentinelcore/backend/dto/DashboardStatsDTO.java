package com.sentinelcore.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    private long totalAssets;

    private long healthyAssets;

    private long runningAssets;

    private long warningAssets;

    private long criticalAssets;

    private long totalAlerts;

    private long criticalAlerts;

    private long totalIncidents;

    private long openIncidents;

    private long totalVulnerabilities;

    private long criticalVulnerabilities;

    private List<ChartData> assetDistribution;

    private List<AlertDTO> recentAlerts;

    private List<AuditLogDTO> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartData {
        private String name;
        private long value;
    }
}