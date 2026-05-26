package com.pyrosense.notification.application.port.in;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.valueobject.AlertSeverity;

public interface ProcessNotificationEventUseCase {

    void processAlertCreated(AlertEventCommand command);

    void processAlertEscalated(AlertEventCommand command);

    void processCriticalRiskDetected(CriticalRiskCommand command);

    void processInterventionCreated(InterventionEventCommand command);

    void processInterventionAssigned(InterventionAssignedCommand command);

    void processInterventionCompleted(InterventionEventCommand command);

    void processReportGenerated(ReportGeneratedCommand command);

    void processDeviceOffline(DeviceOfflineCommand command);

    record AlertEventCommand(
            TenantId tenantId, String alertId, String deviceId,
            AlertSeverity severity, String alertType, String occurredAt
    ) {
        public String fingerprint() {
            return "%s:%s:%s".formatted(alertId, deviceId, severity.name());
        }
    }

    record CriticalRiskCommand(
            TenantId tenantId, String deviceId, String buildingId,
            double riskScore, String occurredAt
    ) {}

    record InterventionEventCommand(
            TenantId tenantId, String interventionId, String buildingId,
            String interventionType, String occurredAt
    ) {}

    record InterventionAssignedCommand(
            TenantId tenantId, String interventionId, String buildingId,
            String assigneeId, String interventionType, String occurredAt
    ) {}

    record ReportGeneratedCommand(
            TenantId tenantId, String reportId, String reportNumber,
            String reportType, String buildingId, String occurredAt
    ) {}

    record DeviceOfflineCommand(
            TenantId tenantId, String deviceId, String buildingId,
            String occurredAt
    ) {}
}
