package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.AlertDTO;
import com.sentinelcore.backend.dto.AuditLogDTO;
import com.sentinelcore.backend.dto.DashboardStatsDTO;
import com.sentinelcore.backend.entity.Asset;
import com.sentinelcore.backend.entity.Alert;
import com.sentinelcore.backend.entity.Vulnerability;
import com.sentinelcore.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AssetRepository assetRepository;
    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardStatsDTO getDashboardStats() {

        List<Asset> assets = assetRepository.findAll();

        long totalAssets = assets.size();

        long healthyAssets = assets.stream()
                .filter(a -> "Healthy".equalsIgnoreCase(a.getStatus()))
                .count();

        long runningAssets = assets.stream()
                .filter(a -> "Running".equalsIgnoreCase(a.getStatus()))
                .count();

        long warningAssets = assets.stream()
                .filter(a -> "Warning".equalsIgnoreCase(a.getStatus()))
                .count();

        long criticalAssets = assets.stream()
                .filter(a -> "Critical".equalsIgnoreCase(a.getStatus()))
                .count();

        long totalAlerts = alertRepository.count();
        long criticalAlerts = alertRepository.findAll().stream()
                .filter(a -> "CRITICAL".equalsIgnoreCase(a.getSeverity()))
                .count();

        long totalIncidents = incidentRepository.count();
        long openIncidents = incidentRepository.findAll().stream()
                .filter(i -> "OPEN".equalsIgnoreCase(i.getStatus()) || "INVESTIGATING".equalsIgnoreCase(i.getStatus()))
                .count();

        List<Vulnerability> vulnerabilities = vulnerabilityRepository.findAll();
        long totalVulnerabilities = vulnerabilities.size();
        long criticalVulnerabilities = vulnerabilities.stream()
                .filter(v -> "CRITICAL".equalsIgnoreCase(v.getSeverity()))
                .count();

        List<DashboardStatsDTO.ChartData> distribution = List.of(
                new DashboardStatsDTO.ChartData("Running", runningAssets),
                new DashboardStatsDTO.ChartData("Healthy", healthyAssets),
                new DashboardStatsDTO.ChartData("Warning", warningAssets),
                new DashboardStatsDTO.ChartData("Critical", criticalAssets)
        );

        // Fetch top 5 recent alerts
        List<AlertDTO> recentAlerts = alertRepository.findAll(
                PageRequest.of(0, 5, Sort.by("createdAt").descending())
        ).stream().map(this::mapAlertToDTO).toList();

        // Fetch top 5 recent activities
        List<AuditLogDTO> recentActivities = auditLogRepository.findAll(
                PageRequest.of(0, 5, Sort.by("timestamp").descending())
        ).stream().map(entity -> AuditLogDTO.builder()
                .id(entity.getId())
                .action(entity.getAction())
                .username(entity.getUsername())
                .details(entity.getDetails())
                .ipAddress(entity.getIpAddress())
                .timestamp(entity.getTimestamp())
                .build()
        ).toList();

        return DashboardStatsDTO.builder()
                .totalAssets(totalAssets)
                .healthyAssets(healthyAssets)
                .runningAssets(runningAssets)
                .warningAssets(warningAssets)
                .criticalAssets(criticalAssets)
                .totalAlerts(totalAlerts)
                .criticalAlerts(criticalAlerts)
                .totalIncidents(totalIncidents)
                .openIncidents(openIncidents)
                .totalVulnerabilities(totalVulnerabilities)
                .criticalVulnerabilities(criticalVulnerabilities)
                .assetDistribution(distribution)
                .recentAlerts(recentAlerts)
                .recentActivities(recentActivities)
                .build();
    }

    private AlertDTO mapAlertToDTO(Alert alert) {
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
}