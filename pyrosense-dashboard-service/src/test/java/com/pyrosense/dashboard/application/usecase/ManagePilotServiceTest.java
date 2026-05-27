package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.in.ManagePilotUseCase.*;
import com.pyrosense.dashboard.application.port.out.PilotRepositoryPort;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagePilotServiceTest {

    @Mock
    private PilotRepositoryPort repository;

    private ManagePilotService service;

    @BeforeEach
    void setUp() {
        service = new ManagePilotService(repository);
    }

    @Test
    void createPilot_shouldSaveAndReturn() {
        var command = new CreatePilotCommand("tenant-1", "Field Pilot 10", "Description");

        PilotProgram result = service.createPilot(command);

        assertThat(result.getName()).isEqualTo("Field Pilot 10");
        assertThat(result.getTenantId()).isEqualTo("tenant-1");
        assertThat(result.getStatus()).isEqualTo(PilotStatus.PREPARING);
        verify(repository).savePilot(any(PilotProgram.class));
    }

    @Test
    void getPilot_shouldReturnWithDevices() {
        UUID pilotId = UUID.randomUUID();
        var pilot = new PilotProgram(pilotId, "tenant-1", "Pilot", "Desc",
                PilotStatus.ACTIVE, Instant.now(), Instant.now(), null);
        var device = PilotDevice.plan(pilotId, "dev-1", "SN-001", "Site A", "Circuit");

        when(repository.findPilotById(pilotId)).thenReturn(Optional.of(pilot));
        when(repository.findDevicesByPilot(pilotId)).thenReturn(List.of(device));

        PilotProgram result = service.getPilot(pilotId, "tenant-1");

        assertThat(result.getDevices()).hasSize(1);
        assertThat(result.getDevices().get(0).deviceId()).isEqualTo("dev-1");
    }

    @Test
    void getPilot_wrongTenant_shouldThrow() {
        UUID pilotId = UUID.randomUUID();
        var pilot = new PilotProgram(pilotId, "tenant-1", "Pilot", "Desc",
                PilotStatus.ACTIVE, Instant.now(), Instant.now(), null);
        when(repository.findPilotById(pilotId)).thenReturn(Optional.of(pilot));

        assertThatThrownBy(() -> service.getPilot(pilotId, "tenant-other"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPilot_notFound_shouldThrow() {
        UUID pilotId = UUID.randomUUID();
        when(repository.findPilotById(pilotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPilot(pilotId, "tenant-1"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateStatus_toActive_shouldActivate() {
        UUID pilotId = UUID.randomUUID();
        var pilot = new PilotProgram(pilotId, "tenant-1", "Pilot", "Desc",
                PilotStatus.PREPARING, Instant.now(), null, null);
        when(repository.findPilotById(pilotId)).thenReturn(Optional.of(pilot));
        when(repository.findDevicesByPilot(pilotId)).thenReturn(List.of());

        PilotProgram result = service.updateStatus(pilotId, "tenant-1", PilotStatus.ACTIVE);

        assertThat(result.getStatus()).isEqualTo(PilotStatus.ACTIVE);
        assertThat(result.getStartedAt()).isNotNull();
        verify(repository).updatePilotStatus(any());
    }

    @Test
    void addDevice_shouldSaveDevice() {
        UUID pilotId = UUID.randomUUID();
        var pilot = new PilotProgram(pilotId, "tenant-1", "Pilot", "Desc",
                PilotStatus.ACTIVE, Instant.now(), Instant.now(), null);
        when(repository.findPilotById(pilotId)).thenReturn(Optional.of(pilot));
        when(repository.findDevicesByPilot(pilotId)).thenReturn(List.of());

        var command = new AddDeviceCommand("device-123", "SN-456", "Site B", "Main panel");
        PilotDevice result = service.addDevice(pilotId, "tenant-1", command);

        assertThat(result.deviceId()).isEqualTo("device-123");
        assertThat(result.serialNumber()).isEqualTo("SN-456");
        assertThat(result.status()).isEqualTo(PilotDevice.PilotDeviceStatus.PLANNED);

        ArgumentCaptor<PilotDevice> captor = ArgumentCaptor.forClass(PilotDevice.class);
        verify(repository).saveDevice(captor.capture());
        assertThat(captor.getValue().pilotId()).isEqualTo(pilotId);
    }

    @Test
    void reportIncident_shouldSaveIncident() {
        UUID pilotId = UUID.randomUUID();
        var pilot = new PilotProgram(pilotId, "tenant-1", "Pilot", "Desc",
                PilotStatus.ACTIVE, Instant.now(), Instant.now(), null);
        when(repository.findPilotById(pilotId)).thenReturn(Optional.of(pilot));
        when(repository.findDevicesByPilot(pilotId)).thenReturn(List.of());

        var command = new ReportIncidentCommand("user-1",
                PilotIncident.IncidentSeverity.HIGH,
                PilotIncident.IncidentCategory.HARDWARE_FAILURE,
                "Capteur offline", "Le capteur ne repond plus depuis 2h",
                "dev-1", "Site A");
        PilotIncident result = service.reportIncident(pilotId, "tenant-1", command);

        assertThat(result.title()).isEqualTo("Capteur offline");
        assertThat(result.severity()).isEqualTo(PilotIncident.IncidentSeverity.HIGH);
        verify(repository).saveIncident(any(PilotIncident.class));
    }

    @Test
    void computeCurrentKpis_shouldAggregateFromDevicesAndIncidents() {
        UUID pilotId = UUID.randomUUID();
        var pilot = new PilotProgram(pilotId, "tenant-1", "Pilot", "Desc",
                PilotStatus.ACTIVE, Instant.now(), Instant.now(), null);

        var dev1 = new PilotDevice(UUID.randomUUID(), pilotId, "d1", "SN-1", "Site A", "C1",
                PilotDevice.PilotDeviceStatus.ACTIVE, Instant.now(), null, null);
        var dev2 = new PilotDevice(UUID.randomUUID(), pilotId, "d2", "SN-2", "Site A", "C2",
                PilotDevice.PilotDeviceStatus.ACTIVE, Instant.now(), null, null);
        var dev3 = new PilotDevice(UUID.randomUUID(), pilotId, "d3", "SN-3", "Site B", "C3",
                PilotDevice.PilotDeviceStatus.OFFLINE, Instant.now(), null, null);

        var incident = PilotIncident.report(pilotId, "user-1",
                PilotIncident.IncidentSeverity.MEDIUM,
                PilotIncident.IncidentCategory.FALSE_POSITIVE,
                "FP Alert", "False alert on device d1", "d1", "Site A");

        when(repository.findPilotById(pilotId)).thenReturn(Optional.of(pilot));
        when(repository.findDevicesByPilot(pilotId)).thenReturn(List.of(dev1, dev2, dev3));
        when(repository.findIncidentsByPilot(pilotId)).thenReturn(List.of(incident));

        PilotKpiSnapshot kpi = service.computeCurrentKpis(pilotId, "tenant-1");

        assertThat(kpi.totalDevices()).isEqualTo(3);
        assertThat(kpi.activeDevices()).isEqualTo(2);
        assertThat(kpi.offlineDevices()).isEqualTo(1);
        assertThat(kpi.incidentsOpen()).isEqualTo(1);
        assertThat(kpi.incidentsResolved()).isZero();
        verify(repository).saveKpiSnapshot(any(PilotKpiSnapshot.class));
    }

    @Test
    void listPilots_shouldDelegateToRepository() {
        var pilots = List.of(
                new PilotProgram(UUID.randomUUID(), "tenant-1", "P1", "D1",
                        PilotStatus.ACTIVE, Instant.now(), Instant.now(), null));
        when(repository.findPilotsByTenant("tenant-1")).thenReturn(pilots);

        List<PilotProgram> result = service.listPilots("tenant-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("P1");
    }
}
