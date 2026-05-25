package com.pyrosense.device.application.usecase;

import com.pyrosense.device.application.port.in.RecordHeartbeatUseCase;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.DeviceId;

public class RecordHeartbeatService implements RecordHeartbeatUseCase {

    private final DeviceRepositoryPort repository;

    public RecordHeartbeatService(DeviceRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public void execute(DeviceId deviceId) {
        var device = repository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device", deviceId.toString()));

        device.recordHeartbeat();
        repository.save(device);
    }
}
