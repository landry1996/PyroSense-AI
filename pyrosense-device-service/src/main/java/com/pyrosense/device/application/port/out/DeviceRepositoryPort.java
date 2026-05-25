package com.pyrosense.device.application.port.out;

import com.pyrosense.device.domain.model.Device;
import com.pyrosense.device.domain.model.DeviceStatus;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.pagination.Page;
import com.pyrosense.shared.pagination.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DeviceRepositoryPort {

    Device save(Device device);

    Optional<Device> findById(DeviceId id);

    Optional<Device> findBySerialNumber(String serialNumber);

    Page<Device> findByTenantId(TenantId tenantId, PageRequest pageRequest);

    Page<Device> findByBuildingId(BuildingId buildingId, TenantId tenantId, PageRequest pageRequest);

    boolean existsBySerialNumber(String serialNumber);

    Map<DeviceStatus, Integer> countByTenantAndStatus(TenantId tenantId);

    List<BuildingId> findDistinctBuildingIdsByTenant(TenantId tenantId);

    Map<DeviceStatus, Integer> countByBuildingIdAndStatus(BuildingId buildingId, TenantId tenantId);
}
