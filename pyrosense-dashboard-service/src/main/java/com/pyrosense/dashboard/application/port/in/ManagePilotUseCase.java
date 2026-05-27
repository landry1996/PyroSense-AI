package com.pyrosense.dashboard.application.port.in;

import com.pyrosense.dashboard.domain.model.*;

import java.util.List;
import java.util.UUID;

public interface ManagePilotUseCase {

    PilotProgram createPilot(CreatePilotCommand command);

    PilotProgram getPilot(UUID pilotId, String tenantId);

    List<PilotProgram> listPilots(String tenantId);

    PilotProgram updateStatus(UUID pilotId, String tenantId, PilotStatus newStatus);

    PilotDevice addDevice(UUID pilotId, String tenantId, AddDeviceCommand command);

    PilotObservation addObservation(UUID pilotId, String tenantId, AddObservationCommand command);

    PilotIncident reportIncident(UUID pilotId, String tenantId, ReportIncidentCommand command);

    List<PilotKpiSnapshot> getKpis(UUID pilotId, String tenantId);

    PilotKpiSnapshot computeCurrentKpis(UUID pilotId, String tenantId);

    record CreatePilotCommand(String tenantId, String name, String description) {}

    record AddDeviceCommand(String deviceId, String serialNumber, String siteName, String circuitDescription) {}

    record AddObservationCommand(String authorId, String authorName,
                                  PilotObservation.ObservationType type, String content,
                                  String deviceId, String siteName) {}

    record ReportIncidentCommand(String reportedBy, PilotIncident.IncidentSeverity severity,
                                  PilotIncident.IncidentCategory category, String title,
                                  String description, String deviceId, String siteName) {}
}
