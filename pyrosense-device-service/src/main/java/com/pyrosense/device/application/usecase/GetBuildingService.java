package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.GetBuildingQuery;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.domain.model.DeviceStatus;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;

import java.util.List;
import java.util.Map;

public class GetBuildingService implements GetBuildingQuery {

    private final DeviceRepositoryPort repository;

    public GetBuildingService(DeviceRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<BuildingSummary> listByTenant(TenantId tenantId) {
        List<BuildingId> buildingIds = repository.findDistinctBuildingIdsByTenant(tenantId);
        return buildingIds.stream()
                .map(buildingId -> buildSummary(buildingId, tenantId))
                .toList();
    }

    @Override
    public BuildingSummary getById(BuildingId buildingId, TenantId tenantId) {
        List<BuildingId> buildingIds = repository.findDistinctBuildingIdsByTenant(tenantId);
        if (!buildingIds.contains(buildingId)) {
            throw new NotFoundException("Building", buildingId.toString());
        }
        return buildSummary(buildingId, tenantId);
    }

    private BuildingSummary buildSummary(BuildingId buildingId, TenantId tenantId) {
        Map<DeviceStatus, Integer> counts = repository.countByBuildingIdAndStatus(buildingId, tenantId);

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        int active = counts.getOrDefault(DeviceStatus.ACTIVE, 0);
        double riskScore = computeRiskScore(counts, total);
        String status = deriveStatus(riskScore);

        // MVP: derive name from buildingId until a buildings table is available
        String name = "Building " + buildingId.value().toString().substring(0, 8);
        String address = null;

        return new BuildingSummary(
                buildingId,
                name,
                address,
                total,
                active,
                riskScore,
                status,
                null
        );
    }

    private double computeRiskScore(Map<DeviceStatus, Integer> counts, int total) {
        if (total == 0) {
            return 0.0;
        }
        int offline = counts.getOrDefault(DeviceStatus.OFFLINE, 0);
        int revoked = counts.getOrDefault(DeviceStatus.REVOKED, 0);
        // Risk score: weighted ratio of problematic devices
        // Offline devices contribute moderately, revoked devices contribute heavily
        double rawScore = ((offline * 50.0) + (revoked * 80.0)) / total;
        return Math.min(100.0, Math.round(rawScore * 10.0) / 10.0);
    }

    private String deriveStatus(double riskScore) {
        if (riskScore > 70) {
            return "CRITICAL";
        } else if (riskScore > 50) {
            return "AT_RISK";
        } else if (riskScore >= 30) {
            return "WATCH";
        }
        return "OK";
    }
}
