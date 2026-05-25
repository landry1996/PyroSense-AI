package com.pyrosense.alerting.application.port.out;

import com.pyrosense.alerting.domain.model.Alert;
import com.pyrosense.alerting.domain.model.AlertStatus;
import com.pyrosense.alerting.domain.model.DeduplicationKey;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.valueobject.AlertSeverity;

import java.util.List;
import java.util.Optional;

public interface AlertRepositoryPort {

    Alert save(Alert alert);

    Optional<Alert> findById(AlertId id);

    List<Alert> findByTenantId(TenantId tenantId);

    List<Alert> findByTenantId(TenantId tenantId, int offset, int limit);

    List<Alert> findByDeviceId(DeviceId deviceId);

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByTenantAndStatus(TenantId tenantId, AlertStatus status);

    List<Alert> findByTenantAndStatus(TenantId tenantId, AlertStatus status, int offset, int limit);

    List<Alert> findByTenantAndSeverity(TenantId tenantId, AlertSeverity severity);

    List<Alert> findByTenantAndSeverity(TenantId tenantId, AlertSeverity severity, int offset, int limit);

    List<Alert> findBySeverity(AlertSeverity severity);

    Optional<Alert> findActiveByDeduplicationKey(DeduplicationKey key);

    List<Alert> findEscalationCandidates();

    List<Alert> findByTenantAndBuildingId(TenantId tenantId, String buildingId, int offset, int limit);

    long countByTenantAndStatus(TenantId tenantId, AlertStatus status);

    long countSlaBreached(TenantId tenantId);
}
