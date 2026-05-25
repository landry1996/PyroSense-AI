package com.pyrosense.alerting.application.port.in;

import com.pyrosense.alerting.domain.model.Alert;
import com.pyrosense.alerting.domain.model.AlertStatus;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.valueobject.AlertSeverity;

import java.util.List;
import java.util.Optional;

public interface GetAlertQuery {

    Optional<Alert> findById(AlertId alertId);

    List<Alert> findByTenant(TenantId tenantId);

    List<Alert> findByDevice(DeviceId deviceId);

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByTenantAndStatus(TenantId tenantId, AlertStatus status);

    List<Alert> findBySeverity(AlertSeverity severity);

    List<Alert> findOpenCritical();

    AlertStatistics getStatistics(TenantId tenantId);

    record AlertStatistics(
            long totalOpen,
            long totalAcknowledged,
            long totalInProgress,
            long totalResolved,
            long criticalOpen,
            long slaBreached
    ) {}
}
