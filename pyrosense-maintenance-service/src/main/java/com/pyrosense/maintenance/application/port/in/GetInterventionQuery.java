package com.pyrosense.maintenance.application.port.in;

import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionStatus;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetInterventionQuery {

    Optional<Intervention> findById(UUID id);

    List<Intervention> findByTenant(TenantId tenantId);

    List<Intervention> findByTenantAndStatus(TenantId tenantId, InterventionStatus status);

    List<Intervention> findByElectrician(UserId electricianId);

    List<Intervention> findByDevice(DeviceId deviceId);

    Optional<Intervention> findByAlert(AlertId alertId);

    InterventionStatistics getStatistics(TenantId tenantId);

    record InterventionStatistics(
            long total,
            long inProgress,
            long completed,
            long falsePositives,
            double averageRiskReduction
    ) {}
}
