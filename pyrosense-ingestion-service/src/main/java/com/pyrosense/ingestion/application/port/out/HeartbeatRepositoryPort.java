package com.pyrosense.ingestion.application.port.out;

import com.pyrosense.ingestion.domain.model.DeviceHeartbeat;

public interface HeartbeatRepositoryPort {

    void save(DeviceHeartbeat heartbeat);
}
