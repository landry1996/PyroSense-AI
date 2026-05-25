package com.pyrosense.maintenance.application.port.out;

import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionStatus;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterventionRepositoryPort {

    Intervention save(Intervention intervention);

    Optional<Intervention> findById(UUID id);

    List<Intervention> findByTenantId(TenantId tenantId);

    List<Intervention> findByTenantIdAndStatus(TenantId tenantId, InterventionStatus status);

    List<Intervention> findByAssignedElectricianId(UserId electricianId);

    List<Intervention> findByDeviceId(DeviceId deviceId);

    Optional<Intervention> findBySourceAlertId(AlertId alertId);

    long countByTenantId(TenantId tenantId);

    long countByTenantIdAndStatus(TenantId tenantId, InterventionStatus status);

    long countFalsePositivesByTenantId(TenantId tenantId);

    long countOverdueByTenantId(TenantId tenantId);
}
