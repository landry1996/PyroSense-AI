package com.pyrosense.dashboard.domain.model;

import java.time.Instant;

public record RiskyBuilding(
        String buildingId,
        String name,
        String address,
        double riskScore,
        String status,
        int openAlerts,
        int criticalAlerts,
        String highestSeverity,
        Instant lastAlertAt) {}
