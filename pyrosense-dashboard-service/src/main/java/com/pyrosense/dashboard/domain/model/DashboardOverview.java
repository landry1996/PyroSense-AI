package com.pyrosense.dashboard.domain.model;

import com.pyrosense.shared.id.TenantId;
import java.time.Instant;

public record DashboardOverview(
        TenantId tenantId,
        int totalBuildings,
        int totalDevices,
        int activeDevices,
        int offlineDevices,
        double averageRiskScore,
        int criticalAlerts,
        int warningAlerts,
        int openInterventions,
        int overdueInterventions,
        Instant lastUpdatedAt) {}
