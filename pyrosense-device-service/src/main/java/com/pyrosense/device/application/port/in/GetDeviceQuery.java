package com.pyrosense.device.application.port.in;

import com.pyrosense.device.domain.model.Device;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.pagination.Page;
import com.pyrosense.shared.pagination.PageRequest;

public interface GetDeviceQuery {

    Device getById(DeviceId deviceId);

    Device getBySerialNumber(String serialNumber);

    Page<Device> listByTenant(TenantId tenantId, PageRequest pageRequest);
}
