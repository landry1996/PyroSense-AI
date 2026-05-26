package com.pyrosense.maintenance.adapter.out.persistence;

import com.pyrosense.maintenance.application.port.out.InterventionRepositoryPort;
import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionResult;
import com.pyrosense.maintenance.domain.model.InterventionStatus;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryInterventionRepository implements InterventionRepositoryPort {

    private final Map<UUID, Intervention> store = new ConcurrentHashMap<>();

    @Override
    public Intervention save(Intervention intervention) {
        store.put(intervention.getId(), intervention);
        return intervention;
    }

    @Override
    public Optional<Intervention> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Intervention> findByTenantId(TenantId tenantId) {
        return store.values().stream()
                .filter(i -> i.getTenantId().equals(tenantId))
                .toList();
    }

    @Override
    public List<Intervention> findByTenantIdAndStatus(TenantId tenantId, InterventionStatus status) {
        return store.values().stream()
                .filter(i -> i.getTenantId().equals(tenantId) && i.getStatus() == status)
                .toList();
    }

    @Override
    public List<Intervention> findByAssignedElectricianId(UserId electricianId) {
        return store.values().stream()
                .filter(i -> electricianId.equals(i.getAssignedElectricianId()))
                .toList();
    }

    @Override
    public List<Intervention> findByDeviceId(DeviceId deviceId) {
        return store.values().stream()
                .filter(i -> i.getDeviceId().equals(deviceId))
                .toList();
    }

    @Override
    public Optional<Intervention> findBySourceAlertId(AlertId alertId) {
        return store.values().stream()
                .filter(i -> i.getSourceAlertId().equals(alertId))
                .findFirst();
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return findByTenantId(tenantId).size();
    }

    @Override
    public long countByTenantIdAndStatus(TenantId tenantId, InterventionStatus status) {
        return findByTenantIdAndStatus(tenantId, status).size();
    }

    @Override
    public long countFalsePositivesByTenantId(TenantId tenantId) {
        return store.values().stream()
                .filter(i -> i.getTenantId().equals(tenantId))
                .filter(i -> i.getResult() == InterventionResult.NO_DEFECT_FOUND)
                .count();
    }

    @Override
    public long countOverdueByTenantId(TenantId tenantId) {
        Instant now = Instant.now();
        return store.values().stream()
                .filter(i -> i.getTenantId().equals(tenantId))
                .filter(i -> i.getStatus() != InterventionStatus.COMPLETED && i.getStatus() != InterventionStatus.CANCELLED)
                .filter(i -> i.getScheduledAt() != null && i.getScheduledAt().isBefore(now))
                .count();
    }
}
