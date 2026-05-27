package com.pyrosense.dashboard.application.port.out;

import com.pyrosense.dashboard.domain.model.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PilotRepositoryPort {

    void savePilot(PilotProgram pilot);

    void updatePilotStatus(PilotProgram pilot);

    Optional<PilotProgram> findPilotById(UUID id);

    List<PilotProgram> findPilotsByTenant(String tenantId);

    void saveDevice(PilotDevice device);

    List<PilotDevice> findDevicesByPilot(UUID pilotId);

    void saveObservation(PilotObservation observation);

    List<PilotObservation> findObservationsByPilot(UUID pilotId);

    void saveIncident(PilotIncident incident);

    List<PilotIncident> findIncidentsByPilot(UUID pilotId);

    void saveKpiSnapshot(PilotKpiSnapshot snapshot);

    List<PilotKpiSnapshot> findKpisByPilot(UUID pilotId);

    void saveSite(PilotSite site);

    List<PilotSite> findSitesByPilot(UUID pilotId);
}
