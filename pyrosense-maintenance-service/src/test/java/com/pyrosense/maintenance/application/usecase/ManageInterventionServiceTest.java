package com.pyrosense.maintenance.application.usecase;

import com.pyrosense.maintenance.adapter.out.persistence.InMemoryInterventionRepository;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
import com.pyrosense.maintenance.domain.model.*;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ManageInterventionServiceTest {

    private ManageInterventionService service;
    private InMemoryInterventionRepository repository;
    private List<DomainEvent> publishedEvents;
    private final Instant now = Instant.parse("2025-03-10T10:00:00Z");

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
        repository = new InMemoryInterventionRepository();
        publishedEvents = new ArrayList<>();
        MaintenanceEventPublisherPort publisher = publishedEvents::add;
        service = new ManageInterventionService(repository, publisher);
    }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    private Intervention createAndSave() {
        var intervention = new Intervention(UUID.randomUUID(), TenantId.generate(),
                AlertId.generate(), DeviceId.generate(),
                InterventionType.PREVENTIVE, InterventionPriority.HIGH, "Test");
        return repository.save(intervention);
    }

    @Test
    void shouldScheduleIntervention() {
        Intervention saved = createAndSave();
        Instant future = now.plus(Duration.ofDays(3));

        Intervention result = service.schedule(saved.getId(), future);

        assertThat(result.getStatus()).isEqualTo(InterventionStatus.PLANNED);
        assertThat(result.getScheduledAt()).isEqualTo(future);
    }

    @Test
    void shouldAssignElectrician() {
        Intervention saved = createAndSave();
        UserId electrician = UserId.generate();
        Instant future = now.plus(Duration.ofDays(1));

        Intervention result = service.assign(saved.getId(), electrician, future);

        assertThat(result.getStatus()).isEqualTo(InterventionStatus.ASSIGNED);
        assertThat(result.getAssignedElectricianId()).isEqualTo(electrician);
    }

    @Test
    void shouldStartIntervention() {
        Intervention saved = createAndSave();
        service.assign(saved.getId(), UserId.generate(), now.plus(Duration.ofDays(1)));

        Intervention result = service.start(saved.getId());

        assertThat(result.getStatus()).isEqualTo(InterventionStatus.IN_PROGRESS);
    }

    @Test
    void shouldAddDiagnostic() {
        Intervention saved = createAndSave();
        service.assign(saved.getId(), UserId.generate(), now.plus(Duration.ofDays(1)));
        service.start(saved.getId());

        FieldDiagnostic diag = new FieldDiagnostic("Loose connection", "IR: 90°C",
                "Replace terminal", "elec-42", now);
        Intervention result = service.addDiagnostic(saved.getId(), diag);

        assertThat(result.getDiagnostic().observations()).isEqualTo("Loose connection");
    }

    @Test
    void shouldCompleteAndPublishEvents() {
        Intervention saved = createAndSave();
        service.assign(saved.getId(), UserId.generate(), now.plus(Duration.ofDays(1)));
        service.start(saved.getId());

        Intervention result = service.complete(saved.getId(), InterventionResult.REPAIRED);

        assertThat(result.getStatus()).isEqualTo(InterventionStatus.COMPLETED);
        assertThat(result.getResult()).isEqualTo(InterventionResult.REPAIRED);
        assertThat(publishedEvents).hasSize(2);
        assertThat(publishedEvents.get(0).eventType()).isEqualTo("maintenance.intervention.completed");
        assertThat(publishedEvents.get(1).eventType()).isEqualTo("maintenance.defect.confirmed");
    }

    @Test
    void shouldPublishFalsePositiveEvent() {
        Intervention saved = createAndSave();
        service.assign(saved.getId(), UserId.generate(), now.plus(Duration.ofDays(1)));
        service.start(saved.getId());

        service.complete(saved.getId(), InterventionResult.NO_DEFECT_FOUND);

        assertThat(publishedEvents).hasSize(2);
        assertThat(publishedEvents.get(1).eventType()).isEqualTo("maintenance.false_positive.confirmed");
    }

    @Test
    void shouldRecordRiskImpact() {
        Intervention saved = createAndSave();
        service.assign(saved.getId(), UserId.generate(), now.plus(Duration.ofDays(1)));
        service.start(saved.getId());
        service.complete(saved.getId(), InterventionResult.REPAIRED);

        RiskImpact impact = new RiskImpact(85, 20, 90);
        Intervention result = service.recordRiskImpact(saved.getId(), impact);

        assertThat(result.getRiskImpact().riskScoreBefore()).isEqualTo(85);
        assertThat(result.getRiskImpact().riskScoreAfter()).isEqualTo(20);
        assertThat(result.getRiskImpact().riskReduction()).isEqualTo(65);
    }

    @Test
    void shouldCancelIntervention() {
        Intervention saved = createAndSave();
        service.assign(saved.getId(), UserId.generate(), now.plus(Duration.ofDays(1)));

        Intervention result = service.cancel(saved.getId());

        assertThat(result.getStatus()).isEqualTo(InterventionStatus.CANCELLED);
    }

    @Test
    void shouldThrowOnNotFound() {
        assertThatThrownBy(() -> service.start(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
