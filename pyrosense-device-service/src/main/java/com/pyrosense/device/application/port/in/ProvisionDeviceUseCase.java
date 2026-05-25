package com.pyrosense.device.application.port.in;

import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;

public interface ProvisionDeviceUseCase {

    record ProvisionDeviceCommand(
            DeviceId deviceId,
            TenantId tenantId,
            BuildingId buildingId,
            ElectricalPanelId panelId
    ) {}

    void execute(ProvisionDeviceCommand command, String actor);
}
