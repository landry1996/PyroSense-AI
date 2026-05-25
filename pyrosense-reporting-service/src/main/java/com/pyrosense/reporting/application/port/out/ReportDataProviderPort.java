package com.pyrosense.reporting.application.port.out;

import com.pyrosense.reporting.domain.model.ReportMetadata;
import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;

public interface ReportDataProviderPort {

    ReportMetadata gatherMetadata(TenantId tenantId, BuildingId buildingId,
                                   ReportType type, Instant periodStart, Instant periodEnd);
}
