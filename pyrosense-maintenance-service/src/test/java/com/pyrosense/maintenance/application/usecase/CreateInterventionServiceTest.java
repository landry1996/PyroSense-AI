package com.pyrosense.maintenance.application.usecase;

import com.pyrosense.maintenance.adapter.out.persistence.InMemoryInterventionRepository;
import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase.CreateInterventionCommand;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionPriority;
import com.pyrosense.maintenance.domain.model.InterventionStatus;
import com.pyrosense.maintenance.domain.model.InterventionType;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CreateInterventionServiceTest {

    private CreateInterventionService service;
    private InMemoryInterventionRepository repository;
    private List<DomainEvent> publishedEvents;

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(Instant.parse("2025-03-10T10:00:00Z"), ZoneId.of("UTC")));
        repository = new InMemoryInterventionRepository();
        publishedEvents = new ArrayList<>();
        MaintenanceEventPublisherPort publisher = publishedEvents::add;
        service = new CreateInterventionService(repository, publisher);
    }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    @Test
    void shouldCreateEmergencyInterventionFromCriticalAlert() {
        var command = new CreateInterventionCommand(
                TenantId.generate(), AlertId.generate(), DeviceId.generate(),
                "CRITICAL", "MICRO_ARC", "Critical arc detected on circuit C7");

        Intervention result = service.createFromAlert(command);

        assertThat(result.getStatus()).isEqualTo(InterventionStatus.CREATED);
        assertThat(result.getType()).isEqualTo(InterventionType.EMERGENCY);
        assertThat(result.getPriority()).isEqualTo(InterventionPriority.URGENT);
        assertThat(publishedEvents).hasSize(1);
        assertThat(publishedEvents.get(0).eventType()).isEqualTo("maintenance.intervention.created");
    }

    @Test
    void shouldCreatePreventiveInterventionFromWarningAlert() {
        var command = new CreateInterventionCommand(
                TenantId.generate(), AlertId.generate(), DeviceId.generate(),
                "WARNING", "TEMPERATURE_RISE", "Temperature drift detected");

        Intervention result = service.createFromAlert(command);

        assertThat(result.getType()).isEqualTo(InterventionType.PREVENTIVE);
        assertThat(result.getPriority()).isEqualTo(InterventionPriority.HIGH);
    }

    @Test
    void shouldRejectDuplicateInterventionForSameAlert() {
        AlertId alertId = AlertId.generate();
        var command = new CreateInterventionCommand(
                TenantId.generate(), alertId, DeviceId.generate(),
                "CRITICAL", "MICRO_ARC", "First intervention");

        service.createFromAlert(command);

        var duplicate = new CreateInterventionCommand(
                TenantId.generate(), alertId, DeviceId.generate(),
                "CRITICAL", "MICRO_ARC", "Duplicate");

        assertThatThrownBy(() -> service.createFromAlert(duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldPersistIntervention() {
        var command = new CreateInterventionCommand(
                TenantId.generate(), AlertId.generate(), DeviceId.generate(),
                "CRITICAL", "OVERLOAD", "Overload on panel A");

        Intervention result = service.createFromAlert(command);

        assertThat(repository.findById(result.getId())).isPresent();
    }
}
