package com.pyrosense.device.application.port.in;

import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.List;

public interface GetBuildingQuery {

    List<BuildingSummary> listByTenant(TenantId tenantId);

    BuildingSummary getById(BuildingId buildingId, TenantId tenantId);

    record BuildingSummary(
            BuildingId buildingId,
            String name,
            String address,
            int totalDevices,
            int activeDevices,
            double riskScore,
            String status,
            Instant lastAlertAt
    ) {}
}
