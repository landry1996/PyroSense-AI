package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.GetDeviceQuery;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.domain.model.Device;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.pagination.Page;
import com.pyrosense.shared.pagination.PageRequest;

public class GetDeviceService implements GetDeviceQuery {

    private final DeviceRepositoryPort repository;

    public GetDeviceService(DeviceRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Device getById(DeviceId deviceId) {
        return repository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device", deviceId.toString()));
    }

    @Override
    public Device getBySerialNumber(String serialNumber) {
        return repository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new NotFoundException("Device", serialNumber));
    }

    @Override
    public Page<Device> listByTenant(TenantId tenantId, PageRequest pageRequest) {
        return repository.findByTenantId(tenantId, pageRequest);
    }

    @Override
    public Page<Device> findByBuildingId(BuildingId buildingId, TenantId tenantId, PageRequest pageRequest) {
        return repository.findByBuildingId(buildingId, tenantId, pageRequest);
    }
}
