package com.pyrosense.reporting.application.port.in;

import com.pyrosense.reporting.domain.model.Report;
import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public interface RequestReportUseCase {

    Report requestMonthlyHealth(MonthlyHealthCommand command);

    Report requestMonitoringCertificate(MonitoringCertificateCommand command);

    Report requestCriticalAlertReport(CriticalAlertReportCommand command);

    Report requestInterventionReport(InterventionReportCommand command);

    record MonthlyHealthCommand(
            TenantId tenantId,
            BuildingId buildingId,
            Instant periodStart,
            Instant periodEnd
    ) {}

    record MonitoringCertificateCommand(
            TenantId tenantId,
            BuildingId buildingId,
            Instant periodStart,
            Instant periodEnd
    ) {}

    record CriticalAlertReportCommand(
            TenantId tenantId,
            BuildingId buildingId,
            UUID alertId,
            Instant periodStart,
            Instant periodEnd
    ) {}

    record InterventionReportCommand(
            TenantId tenantId,
            BuildingId buildingId,
            UUID interventionId,
            Instant periodStart,
            Instant periodEnd
    ) {}
}
