package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.in.ManagePilotUseCase;
import com.pyrosense.dashboard.application.port.out.PilotRepositoryPort;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.shared.exception.NotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ManagePilotService implements ManagePilotUseCase {

    private final PilotRepositoryPort repository;

    public ManagePilotService(PilotRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public PilotProgram createPilot(CreatePilotCommand command) {
        var pilot = new PilotProgram(UUID.randomUUID(), command.tenantId(),
                command.name(), command.description());
        repository.savePilot(pilot);
        return pilot;
    }

    @Override
    public PilotProgram getPilot(UUID pilotId, String tenantId) {
        var pilot = repository.findPilotById(pilotId)
                .orElseThrow(() -> new NotFoundException("PilotProgram", pilotId));
        if (!pilot.getTenantId().equals(tenantId)) {
            throw new NotFoundException("PilotProgram", pilotId);
        }
        pilot.getDevices().addAll(repository.findDevicesByPilot(pilotId));
        return pilot;
    }

    @Override
    public List<PilotProgram> listPilots(String tenantId) {
        return repository.findPilotsByTenant(tenantId);
    }

    @Override
    public PilotProgram updateStatus(UUID pilotId, String tenantId, PilotStatus newStatus) {
        var pilot = getPilot(pilotId, tenantId);
        switch (newStatus) {
            case ACTIVE -> pilot.activate();
            case PAUSED -> pilot.pause();
            case COMPLETED -> pilot.complete();
            case CANCELLED -> pilot.cancel();
            default -> throw new IllegalArgumentException("Cannot transition to " + newStatus);
        }
        repository.updatePilotStatus(pilot);
        return pilot;
    }

    @Override
    public PilotDevice addDevice(UUID pilotId, String tenantId, AddDeviceCommand command) {
        getPilot(pilotId, tenantId);
        var device = PilotDevice.plan(pilotId, command.deviceId(),
                command.serialNumber(), command.siteName(), command.circuitDescription());
        repository.saveDevice(device);
        return device;
    }

    @Override
    public PilotObservation addObservation(UUID pilotId, String tenantId, AddObservationCommand command) {
        getPilot(pilotId, tenantId);
        var observation = PilotObservation.create(pilotId, command.authorId(), command.authorName(),
                command.type(), command.content(), command.deviceId(), command.siteName());
        repository.saveObservation(observation);
        return observation;
    }

    @Override
    public PilotIncident reportIncident(UUID pilotId, String tenantId, ReportIncidentCommand command) {
        getPilot(pilotId, tenantId);
        var incident = PilotIncident.report(pilotId, command.reportedBy(),
                command.severity(), command.category(), command.title(),
                command.description(), command.deviceId(), command.siteName());
        repository.saveIncident(incident);
        return incident;
    }

    @Override
    public List<PilotKpiSnapshot> getKpis(UUID pilotId, String tenantId) {
        getPilot(pilotId, tenantId);
        return repository.findKpisByPilot(pilotId);
    }

    @Override
    public PilotKpiSnapshot computeCurrentKpis(UUID pilotId, String tenantId) {
        var pilot = getPilot(pilotId, tenantId);
        var devices = repository.findDevicesByPilot(pilotId);
        var incidents = repository.findIncidentsByPilot(pilotId);

        int total = devices.size();
        int active = (int) devices.stream()
                .filter(d -> d.status() == PilotDevice.PilotDeviceStatus.ACTIVE).count();
        int offline = (int) devices.stream()
                .filter(d -> d.status() == PilotDevice.PilotDeviceStatus.OFFLINE).count();
        int incidentsOpen = (int) incidents.stream()
                .filter(i -> i.status() == PilotIncident.IncidentStatus.OPEN
                        || i.status() == PilotIncident.IncidentStatus.INVESTIGATING).count();
        int incidentsResolved = (int) incidents.stream()
                .filter(i -> i.status() == PilotIncident.IncidentStatus.RESOLVED
                        || i.status() == PilotIncident.IncidentStatus.CLOSED).count();

        double uptimePercent = total > 0 ? (double) active / total * 100.0 : 0;

        var snapshot = PilotKpiSnapshot.compute(pilotId, LocalDate.now(),
                total, active, offline, uptimePercent, 99.0, 75.0,
                0, 0, 0, incidentsOpen, incidentsResolved, 0);
        repository.saveKpiSnapshot(snapshot);
        return snapshot;
    }
}
