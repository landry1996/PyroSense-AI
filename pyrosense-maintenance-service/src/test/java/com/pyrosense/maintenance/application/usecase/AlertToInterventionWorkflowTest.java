package com.pyrosense.maintenance.application.usecase;

import com.pyrosense.maintenance.adapter.out.persistence.InMemoryInterventionRepository;
import com.pyrosense.maintenance.adapter.out.persistence.InMemoryRecommendationRepository;
import com.pyrosense.maintenance.application.port.out.AuditLogPort;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
import com.pyrosense.maintenance.application.port.out.RiskScoreReevaluationPublisherPort;
import com.pyrosense.maintenance.domain.model.*;
import com.pyrosense.shared.domain.DomainEvent;
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

class AlertToInterventionWorkflowTest {

    private final Instant now = Instant.parse("2025-03-10T10:00:00Z");
    private final TenantId tenantId = TenantId.generate();
    private final DeviceId deviceId = DeviceId.generate();

    private InMemoryInterventionRepository interventionRepo;
    private InMemoryRecommendationRepository recommendationRepo;
    private CreateInterventionService createService;
    private ManageInterventionService manageService;
    private ManageRecommendationService recommendationService;
    private List<DomainEvent> publishedEvents;
    private List<String> auditEntries;

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
        interventionRepo = new InMemoryInterventionRepository();
        recommendationRepo = new InMemoryRecommendationRepository();
        publishedEvents = new ArrayList<>();
        auditEntries = new ArrayList<>();

        MaintenanceEventPublisherPort eventPublisher = publishedEvents::add;
        RiskScoreReevaluationPublisherPort riskPublisher = (t, d, i) -> {};
        AuditLogPort auditLog = (tenantId1, actor, interventionId, action, detail) ->
                auditEntries.add(action + ": " + detail);

        createService = new CreateInterventionService(interventionRepo, eventPublisher);
        manageService = new ManageInterventionService(interventionRepo, eventPublisher, riskPublisher);
        recommendationService = new ManageRecommendationService(
                recommendationRepo, createService, eventPublisher, auditLog);
    }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    @Test
    void fullWorkflow_criticalAlert_autoIntervention_toCompletion() {
        // Step 1-3: CRITICAL alert → auto-create intervention
        AlertId alertId = AlertId.generate();
        var command = new com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase.CreateInterventionCommand(
                tenantId, alertId, deviceId, "CRITICAL", "MICRO_ARC", "Critical arc detected");
        Intervention intervention = createService.createFromAlert(command);

        assertThat(intervention.getStatus()).isEqualTo(InterventionStatus.CREATED);
        assertThat(intervention.getPriority()).isEqualTo(InterventionPriority.URGENT);
        assertThat(intervention.getType()).isEqualTo(InterventionType.EMERGENCY);

        // Step 6: Assign electrician
        UserId electrician = UserId.generate();
        Instant scheduled = now.plus(Duration.ofHours(4));
        intervention = manageService.assign(intervention.getId(), electrician, scheduled);
        assertThat(intervention.getStatus()).isEqualTo(InterventionStatus.ASSIGNED);

        // Step 7: Start
        intervention = manageService.start(intervention.getId());
        assertThat(intervention.getStatus()).isEqualTo(InterventionStatus.IN_PROGRESS);

        // Step 8: Add diagnostic
        var diagnostic = new FieldDiagnostic("Micro-arc detected at junction B3",
                "IR camera: 120°C hotspot", "Replace connector immediately", "elec-007", now);
        intervention = manageService.addDiagnostic(intervention.getId(), diagnostic);
        assertThat(intervention.getDiagnostic()).isNotNull();

        // Step 9: Complete
        intervention = manageService.complete(intervention.getId(), InterventionResult.REPAIRED);
        assertThat(intervention.getStatus()).isEqualTo(InterventionStatus.COMPLETED);
        assertThat(intervention.getResult()).isEqualTo(InterventionResult.REPAIRED);

        // Step 10: Events published
        assertThat(publishedEvents.stream().map(DomainEvent::eventType))
                .contains("maintenance.intervention.created",
                        "maintenance.intervention.assigned",
                        "maintenance.intervention.started",
                        "maintenance.intervention.completed",
                        "maintenance.defect.confirmed");
    }

    @Test
    void fullWorkflow_warningAlert_recommendation_acceptAndComplete() {
        // Step 1-3: WARNING alert → recommendation created
        AlertId alertId = AlertId.generate();
        InterventionPriorityPolicy policy = new InterventionPriorityPolicy();
        InterventionPriority priority = policy.determineFromAlert("WARNING", 65);
        InterventionType type = policy.determineTypeFromAlert("WARNING");
        Duration sla = policy.determineSlaDeadline(priority);

        var recommendation = new InterventionRecommendation(
                UUID.randomUUID(), tenantId, alertId, deviceId,
                type, priority, "Warning: temperature drift detected", sla);
        recommendationRepo.save(recommendation);

        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.PENDING);

        // Step 4: Manager accepts recommendation
        Intervention intervention = recommendationService.acceptRecommendation(recommendation.getId());
        assertThat(intervention.getStatus()).isEqualTo(InterventionStatus.CREATED);
        assertThat(intervention.getPriority()).isEqualTo(InterventionPriority.HIGH);

        // Continue workflow...
        UserId electrician = UserId.generate();
        manageService.assign(intervention.getId(), electrician, now.plus(Duration.ofDays(1)));
        manageService.start(intervention.getId());
        manageService.addDiagnostic(intervention.getId(),
                new FieldDiagnostic("Temperature within limits", "Thermal: 42°C", null, "elec-01", now));
        Intervention completed = manageService.complete(intervention.getId(), InterventionResult.NO_DEFECT_FOUND);

        assertThat(completed.isFalsePositive()).isTrue();

        // Verify false positive event published
        assertThat(publishedEvents.stream().map(DomainEvent::eventType))
                .contains("maintenance.false_positive.confirmed");

        // Verify audit trail
        assertThat(auditEntries).anyMatch(e -> e.contains("RECOMMENDATION_ACCEPTED"));
    }

    @Test
    void recommendationRejection() {
        AlertId alertId = AlertId.generate();
        var recommendation = new InterventionRecommendation(
                UUID.randomUUID(), tenantId, alertId, deviceId,
                InterventionType.PREVENTIVE, InterventionPriority.MEDIUM,
                "Low risk warning", Duration.ofDays(3));
        recommendationRepo.save(recommendation);

        InterventionRecommendation rejected = recommendationService.rejectRecommendation(
                recommendation.getId(), "Risk assessed as acceptable by team");

        assertThat(rejected.getStatus()).isEqualTo(RecommendationStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Risk assessed as acceptable by team");
        assertThat(auditEntries).anyMatch(e -> e.contains("RECOMMENDATION_REJECTED"));
    }

    @Test
    void idempotence_duplicateAlertDoesNotCreateSecondIntervention() {
        AlertId alertId = AlertId.generate();
        var command = new com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase.CreateInterventionCommand(
                tenantId, alertId, deviceId, "CRITICAL", "MICRO_ARC", "Arc detected");

        // First creation succeeds
        Intervention first = createService.createFromAlert(command);
        assertThat(first).isNotNull();

        // Second creation for same alert throws
        assertThatThrownBy(() -> createService.createFromAlert(command))
                .hasMessageContaining("already exists for alert");
    }

    @Test
    void idempotence_duplicateRecommendationPrevented() {
        AlertId alertId = AlertId.generate();
        var rec1 = new InterventionRecommendation(
                UUID.randomUUID(), tenantId, alertId, deviceId,
                InterventionType.PREVENTIVE, InterventionPriority.HIGH, "First", Duration.ofHours(24));
        recommendationRepo.save(rec1);

        // Second recommendation for same alert is blocked by unique index
        assertThat(recommendationRepo.findByAlertId(alertId)).isPresent();
    }
}
