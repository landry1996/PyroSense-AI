package com.pyrosense.reporting.domain.model;

import java.util.List;
import java.util.Objects;

public record ReportMetadata(
        String buildingName,
        String buildingAddress,
        int sensorCount,
        int offlineSensorCount,
        double averageRiskScore,
        int alertCount,
        int criticalAlertCount,
        int interventionCount,
        int resolvedInterventionCount,
        int confirmedDefects,
        int falsePositives,
        double riskEvolutionPercent,
        List<String> topRiskBuildings,
        List<String> recommendations,
        String roiSummary
) {
    public ReportMetadata {
        Objects.requireNonNull(buildingName);
        if (sensorCount < 0) throw new IllegalArgumentException("sensorCount must be >= 0");
        if (averageRiskScore < 0 || averageRiskScore > 100) throw new IllegalArgumentException("averageRiskScore must be 0-100");
    }

    public ReportMetadata(String buildingName, String buildingAddress, int sensorCount,
                           double averageRiskScore, int alertCount, int criticalAlertCount,
                           int interventionCount, int resolvedInterventionCount,
                           List<String> recommendations) {
        this(buildingName, buildingAddress, sensorCount, 0, averageRiskScore,
                alertCount, criticalAlertCount, interventionCount, resolvedInterventionCount,
                0, 0, 0.0, List.of(), recommendations, null);
    }
}

