package com.pyrosense.dashboard.application.port.out;

import com.pyrosense.dashboard.domain.model.DeviceSecurityStatus;
import com.pyrosense.dashboard.domain.model.DeviceTechnicalHealth;
import com.pyrosense.dashboard.domain.model.PilotDashboard.PilotDeviceSummary;
import com.pyrosense.dashboard.domain.model.TelemetryQualityReport;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTechnicalReadModelPort {

    Optional<DeviceTechnicalHealth> findTechnicalHealth(String deviceId);

    TelemetryQualityReport findTelemetryQuality(String deviceId, Instant from, Instant to);

    DeviceSecurityStatus findSecurityStatus(String deviceId);

    List<PilotDeviceSummary> findPilotDeviceSummaries(UUID pilotId);
}
