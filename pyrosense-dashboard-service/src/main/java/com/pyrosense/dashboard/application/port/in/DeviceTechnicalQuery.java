package com.pyrosense.dashboard.application.port.in;

import com.pyrosense.dashboard.domain.model.DeviceSecurityStatus;
import com.pyrosense.dashboard.domain.model.DeviceTechnicalHealth;
import com.pyrosense.dashboard.domain.model.PilotDashboard;
import com.pyrosense.dashboard.domain.model.TelemetryQualityReport;

import java.time.Instant;
import java.util.UUID;

public interface DeviceTechnicalQuery {

    DeviceTechnicalHealth getDeviceTechnicalHealth(String deviceId, String tenantId);

    TelemetryQualityReport getTelemetryQuality(String deviceId, String tenantId, Instant from, Instant to);

    DeviceSecurityStatus getDeviceSecurityStatus(String deviceId, String tenantId);

    PilotDashboard getPilotDashboard(UUID pilotId, String tenantId);
}
