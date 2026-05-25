package com.pyrosense.alerting.application.usecase;

import com.pyrosense.alerting.application.port.in.GetAlertQuery;
import com.pyrosense.alerting.application.port.out.AlertRepositoryPort;
import com.pyrosense.alerting.domain.model.Alert;
import com.pyrosense.alerting.domain.model.AlertStatus;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.valueobject.AlertSeverity;

import java.util.List;
import java.util.Optional;

public class GetAlertService implements GetAlertQuery {

    private final AlertRepositoryPort repository;

    public GetAlertService(AlertRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Alert> findById(AlertId alertId) {
        return repository.findById(alertId);
    }

    @Override
    public List<Alert> findByTenant(TenantId tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @Override
    public List<Alert> findByDevice(DeviceId deviceId) {
        return repository.findByDeviceId(deviceId);
    }

    @Override
    public List<Alert> findByStatus(AlertStatus status) {
        return repository.findByStatus(status);
    }

    @Override
    public List<Alert> findByTenantAndStatus(TenantId tenantId, AlertStatus status) {
        return repository.findByTenantAndStatus(tenantId, status);
    }

    @Override
    public List<Alert> findBySeverity(AlertSeverity severity) {
        return repository.findBySeverity(severity);
    }

    @Override
    public List<Alert> findOpenCritical() {
        return repository.findBySeverity(AlertSeverity.CRITICAL).stream()
                .filter(a -> !a.status().isTerminal())
                .toList();
    }

    @Override
    public AlertStatistics getStatistics(TenantId tenantId) {
        long open = repository.countByTenantAndStatus(tenantId, AlertStatus.OPEN);
        long ack = repository.countByTenantAndStatus(tenantId, AlertStatus.ACKNOWLEDGED);
        long inProgress = repository.countByTenantAndStatus(tenantId, AlertStatus.IN_PROGRESS);
        long resolved = repository.countByTenantAndStatus(tenantId, AlertStatus.RESOLVED);
        long criticalOpen = repository.findByTenantAndStatus(tenantId, AlertStatus.OPEN).stream()
                .filter(a -> a.severity() == AlertSeverity.CRITICAL)
                .count();
        long slaBreached = repository.countSlaBreached(tenantId);

        return new AlertStatistics(open, ack, inProgress, resolved, criticalOpen, slaBreached);
    }
}
