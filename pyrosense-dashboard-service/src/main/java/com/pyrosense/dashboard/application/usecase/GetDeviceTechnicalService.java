package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.in.DeviceTechnicalQuery;
import com.pyrosense.dashboard.application.port.out.DeviceTechnicalReadModelPort;
import com.pyrosense.dashboard.application.port.out.PilotRepositoryPort;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.dashboard.domain.model.PilotDashboard.IncidentsSummary;
import com.pyrosense.dashboard.domain.model.PilotDashboard.PilotDeviceSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GetDeviceTechnicalService implements DeviceTechnicalQuery {

    private final DeviceTechnicalReadModelPort readModel;
    private final PilotRepositoryPort pilotRepository;

    public GetDeviceTechnicalService(DeviceTechnicalReadModelPort readModel,
                                     PilotRepositoryPort pilotRepository) {
        this.readModel = readModel;
        this.pilotRepository = pilotRepository;
    }

    @Override
    public DeviceTechnicalHealth getDeviceTechnicalHealth(String deviceId, String tenantId) {
        return readModel.findTechnicalHealth(deviceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Device not found: " + deviceId));
    }

    @Override
    public TelemetryQualityReport getTelemetryQuality(String deviceId, String tenantId,
                                                       Instant from, Instant to) {
        return readModel.findTelemetryQuality(deviceId, from, to);
    }

    @Override
    public DeviceSecurityStatus getDeviceSecurityStatus(String deviceId, String tenantId) {
        return readModel.findSecurityStatus(deviceId);
    }

    @Override
    public PilotDashboard getPilotDashboard(UUID pilotId, String tenantId) {
        PilotProgram pilot = pilotRepository.findPilotById(pilotId)
                .orElseThrow(() -> new IllegalArgumentException("Pilot not found: " + pilotId));

        if (!pilot.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Pilot not found: " + pilotId);
        }

        List<PilotDeviceSummary> devices = readModel.findPilotDeviceSummaries(pilotId);

        List<PilotIncident> incidents = pilotRepository.findIncidentsByPilot(pilotId);
        int open = (int) incidents.stream()
                .filter(i -> i.status() == PilotIncident.IncidentStatus.OPEN).count();
        int investigating = (int) incidents.stream()
                .filter(i -> i.status() == PilotIncident.IncidentStatus.INVESTIGATING).count();
        int resolved = (int) incidents.stream()
                .filter(i -> i.status() == PilotIncident.IncidentStatus.RESOLVED
                        || i.status() == PilotIncident.IncidentStatus.CLOSED).count();
        IncidentsSummary incidentsSummary = new IncidentsSummary(open, investigating, resolved, incidents.size());

        List<PilotKpiSnapshot> kpis = pilotRepository.findKpisByPilot(pilotId);
        PilotKpiSnapshot latestKpi = kpis.isEmpty() ? null :
                kpis.stream()
                        .max((a, b) -> a.computedAt().compareTo(b.computedAt()))
                        .orElse(null);

        return new PilotDashboard(
                pilot.getId(),
                pilot.getName(),
                pilot.getStatus().name(),
                devices,
                incidentsSummary,
                latestKpi
        );
    }
}
