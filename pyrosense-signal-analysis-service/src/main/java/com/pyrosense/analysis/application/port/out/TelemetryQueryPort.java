package com.pyrosense.analysis.application.port.out;

import com.pyrosense.analysis.domain.model.SignalWindow;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Duration;
import java.util.Optional;

public interface TelemetryQueryPort {
    Optional<SignalWindow> getWindow(DeviceId deviceId, TenantId tenantId, Duration windowSize);
}
