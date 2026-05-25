package com.pyrosense.maintenance.application.usecase;

import com.pyrosense.maintenance.application.port.in.GetInterventionQuery;
import com.pyrosense.maintenance.application.port.out.InterventionRepositoryPort;
import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionStatus;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GetInterventionService implements GetInterventionQuery {

    private final InterventionRepositoryPort repository;

    public GetInterventionService(InterventionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Intervention> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Intervention> findByTenant(TenantId tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @Override
    public List<Intervention> findByTenantAndStatus(TenantId tenantId, InterventionStatus status) {
        return repository.findByTenantIdAndStatus(tenantId, status);
    }

    @Override
    public List<Intervention> findByElectrician(UserId electricianId) {
        return repository.findByAssignedElectricianId(electricianId);
    }

    @Override
    public List<Intervention> findByDevice(DeviceId deviceId) {
        return repository.findByDeviceId(deviceId);
    }

    @Override
    public Optional<Intervention> findByAlert(AlertId alertId) {
        return repository.findBySourceAlertId(alertId);
    }

    @Override
    public InterventionStatistics getStatistics(TenantId tenantId) {
        long total = repository.countByTenantId(tenantId);
        long inProgress = repository.countByTenantIdAndStatus(tenantId, InterventionStatus.IN_PROGRESS);
        long completed = repository.countByTenantIdAndStatus(tenantId, InterventionStatus.COMPLETED);
        long falsePositives = repository.countFalsePositivesByTenantId(tenantId);

        List<Intervention> completedInterventions = repository.findByTenantIdAndStatus(tenantId, InterventionStatus.COMPLETED);
        double averageRiskReduction = completedInterventions.stream()
                .filter(i -> i.getRiskImpact() != null)
                .mapToInt(i -> i.getRiskImpact().riskReduction())
                .average()
                .orElse(0.0);

        return new InterventionStatistics(total, inProgress, completed, falsePositives, averageRiskReduction);
    }
}
