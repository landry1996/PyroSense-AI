package com.pyrosense.maintenance.domain.model;

import com.pyrosense.shared.exception.InvalidStateTransitionException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class InterventionTest {

    private final Instant now = Instant.parse("2025-03-10T10:00:00Z");
    private final TenantId tenantId = TenantId.generate();
    private final AlertId alertId = AlertId.generate();
    private final DeviceId deviceId = DeviceId.generate();

    @BeforeEach
    void setUp() { ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC"))); }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    private Intervention createIntervention() {
        return new Intervention(UUID.randomUUID(), tenantId, alertId, deviceId,
                InterventionType.PREVENTIVE, InterventionPriority.HIGH, "Test intervention");
    }

    @Test
    void shouldCreateWithInitialState() {
        Intervention i = createIntervention();

        assertThat(i.getStatus()).isEqualTo(InterventionStatus.CREATED);
        assertThat(i.getTenantId()).isEqualTo(tenantId);
        assertThat(i.getSourceAlertId()).isEqualTo(alertId);
        assertThat(i.getDeviceId()).isEqualTo(deviceId);
        assertThat(i.getType()).isEqualTo(InterventionType.PREVENTIVE);
        assertThat(i.getPriority()).isEqualTo(InterventionPriority.HIGH);
        assertThat(i.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldSchedule() {
        Intervention i = createIntervention();
        Instant future = now.plus(Duration.ofDays(2));

        i.schedule(future);

        assertThat(i.getStatus()).isEqualTo(InterventionStatus.PLANNED);
        assertThat(i.getScheduledAt()).isEqualTo(future);
    }

    @Test
    void shouldRejectScheduleInPast() {
        Intervention i = createIntervention();
        Instant past = now.minus(Duration.ofHours(1));

        assertThatThrownBy(() -> i.schedule(past))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    void shouldAssignElectrician() {
        Intervention i = createIntervention();
        UserId electrician = UserId.generate();
        Instant future = now.plus(Duration.ofDays(1));

        i.assign(electrician, future);

        assertThat(i.getStatus()).isEqualTo(InterventionStatus.ASSIGNED);
        assertThat(i.getAssignedElectricianId()).isEqualTo(electrician);
        assertThat(i.getScheduledAt()).isEqualTo(future);
    }

    @Test
    void shouldStart() {
        Intervention i = createIntervention();
        i.assign(UserId.generate(), now.plus(Duration.ofDays(1)));

        i.start();

        assertThat(i.getStatus()).isEqualTo(InterventionStatus.IN_PROGRESS);
        assertThat(i.getStartedAt()).isEqualTo(now);
    }

    @Test
    void shouldAddDiagnosticWhenInProgress() {
        Intervention i = createIntervention();
        i.assign(UserId.generate(), now.plus(Duration.ofDays(1)));
        i.start();

        FieldDiagnostic diag = new FieldDiagnostic("Loose connection at panel B3",
                "IR camera: 85°C hotspot", "Replace connector", "elec-001", now);
        i.addDiagnostic(diag);

        assertThat(i.getDiagnostic()).isEqualTo(diag);
    }

    @Test
    void shouldRejectDiagnosticWhenNotInProgress() {
        Intervention i = createIntervention();

        assertThatThrownBy(() -> i.addDiagnostic(
                new FieldDiagnostic("obs", null, null, "elec", now)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void shouldComplete() {
        Intervention i = createIntervention();
        i.assign(UserId.generate(), now.plus(Duration.ofDays(1)));
        i.start();
        i.addDiagnostic(new FieldDiagnostic("obs", "measure", "rec", "elec", now));

        i.complete(InterventionResult.REPAIRED);

        assertThat(i.getStatus()).isEqualTo(InterventionStatus.COMPLETED);
        assertThat(i.getResult()).isEqualTo(InterventionResult.REPAIRED);
        assertThat(i.getCompletedAt()).isEqualTo(now);
        assertThat(i.isDefectConfirmed()).isTrue();
        assertThat(i.isFalsePositive()).isFalse();
    }

    @Test
    void shouldDetectFalsePositive() {
        Intervention i = createIntervention();
        i.assign(UserId.generate(), now.plus(Duration.ofDays(1)));
        i.start();
        i.addDiagnostic(new FieldDiagnostic("obs", null, null, "elec", now));

        i.complete(InterventionResult.NO_DEFECT_FOUND);

        assertThat(i.isFalsePositive()).isTrue();
        assertThat(i.isDefectConfirmed()).isFalse();
    }

    @Test
    void shouldRecordRiskImpact() {
        Intervention i = createIntervention();
        i.assign(UserId.generate(), now.plus(Duration.ofDays(1)));
        i.start();
        i.addDiagnostic(new FieldDiagnostic("obs", "measure", "rec", "elec", now));
        i.complete(InterventionResult.REPAIRED);

        RiskImpact impact = new RiskImpact(75, 20, 45);
        i.recordRiskImpact(impact);

        assertThat(i.getRiskImpact()).isEqualTo(impact);
        assertThat(i.getRiskImpact().riskReduction()).isEqualTo(55);
        assertThat(i.getRiskImpact().hasImprovement()).isTrue();
    }

    @Test
    void shouldRejectRiskImpactWhenNotCompleted() {
        Intervention i = createIntervention();

        assertThatThrownBy(() -> i.recordRiskImpact(new RiskImpact(50, 30, null)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void shouldCancel() {
        Intervention i = createIntervention();
        i.assign(UserId.generate(), now.plus(Duration.ofDays(1)));

        i.cancel("Client requested cancellation");

        assertThat(i.getStatus()).isEqualTo(InterventionStatus.CANCELLED);
        assertThat(i.getCancellationReason()).isEqualTo("Client requested cancellation");
    }

    @Test
    void shouldRejectTransitionFromCompleted() {
        Intervention i = createIntervention();
        i.assign(UserId.generate(), now.plus(Duration.ofDays(1)));
        i.start();
        i.addDiagnostic(new FieldDiagnostic("obs", "measure", "rec", "elec", now));
        i.complete(InterventionResult.REPAIRED);

        assertThatThrownBy(() -> i.cancel("should fail"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void shouldRejectStartFromCreatedDirectly() {
        Intervention i = createIntervention();

        assertThatThrownBy(i::start)
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void shouldDetectFollowUpNeeded() {
        Intervention i = createIntervention();
        i.assign(UserId.generate(), now.plus(Duration.ofDays(1)));
        i.start();
        i.addDiagnostic(new FieldDiagnostic("obs", null, null, "elec", now));
        i.complete(InterventionResult.NEEDS_FOLLOW_UP);

        assertThat(i.requiresFollowUp()).isTrue();
    }
}
