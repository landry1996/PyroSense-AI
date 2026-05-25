package com.pyrosense.reporting.application.port.in;

import com.pyrosense.reporting.domain.model.Report;
import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;

public interface GenerateReportUseCase {

    Report generate(GenerateReportCommand command);

    record GenerateReportCommand(
            TenantId tenantId,
            BuildingId buildingId,
            ReportType type,
            Instant periodStart,
            Instant periodEnd
    ) {}
}
