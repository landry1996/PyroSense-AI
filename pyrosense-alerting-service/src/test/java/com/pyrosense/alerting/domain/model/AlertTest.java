package com.pyrosense.alerting.domain.model;

import com.pyrosense.alerting.domain.event.*;
import com.pyrosense.shared.exception.InvalidStateTransitionException;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

class AlertTest {

    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final TenantId tenantId = TenantId.generate();
    private final DeviceId deviceId = DeviceId.generate();
    private final UserId userId = UserId.generate();

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    private Alert createAlert(AlertSeverity severity) {
        return Alert.create(tenantId, deviceId, AlertType.MICRO_ARC_DETECTED, severity,
                "Test alert", "Test description", SlaPolicy.defaults());
    }

    @Test
    void shouldCreateAlertWithCorrectState() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);

        assertThat(alert.getId()).isNotNull();
        assertThat(alert.tenantId()).isEqualTo(tenantId);
        assertThat(alert.deviceId()).isEqualTo(deviceId);
        assertThat(alert.type()).isEqualTo(AlertType.MICRO_ARC_DETECTED);
        assertThat(alert.severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(alert.status()).isEqualTo(AlertStatus.OPEN);
        assertThat(alert.escalationLevel()).isEqualTo(EscalationLevel.NONE);
        assertThat(alert.occurrenceCount()).isEqualTo(1);
        assertThat(alert.createdAt()).isEqualTo(now);
        assertThat(alert.slaDeadline()).isEqualTo(now.plus(Duration.ofHours(24)));
    }

    @Test
    void shouldRegisterCreatedEvent() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);

        assertThat(alert.getDomainEvents()).hasSize(1);
        assertThat(alert.getDomainEvents().get(0)).isInstanceOf(AlertCreatedEvent.class);
    }

    @Test
    void shouldAcknowledge() {
        Alert alert = createAlert(AlertSeverity.WARNING);
        alert.clearDomainEvents();

        alert.acknowledge(userId);

        assertThat(alert.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(alert.acknowledgedAt()).isEqualTo(now);
        assertThat(alert.acknowledgedBy()).isEqualTo(userId);
        assertThat(alert.getDomainEvents()).hasSize(1);
        assertThat(alert.getDomainEvents().get(0)).isInstanceOf(AlertAcknowledgedEvent.class);
    }

    @Test
    void shouldNotAcknowledgeResolvedAlert() {
        Alert alert = createAlert(AlertSeverity.WARNING);
        alert.resolve(userId, "fixed");

        assertThatThrownBy(() -> alert.acknowledge(userId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void shouldAssignAndTransitionToInProgress() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);
        UserId assignee = UserId.generate();
        alert.clearDomainEvents();

        alert.assign(assignee, userId);

        assertThat(alert.status()).isEqualTo(AlertStatus.IN_PROGRESS);
        assertThat(alert.assignedTo()).isEqualTo(assignee);
        assertThat(alert.getDomainEvents()).hasSize(1);
        assertThat(alert.getDomainEvents().get(0)).isInstanceOf(AlertAssignedEvent.class);
    }

    @Test
    void shouldNotAssignResolvedAlert() {
        Alert alert = createAlert(AlertSeverity.WARNING);
        alert.resolve(userId, "done");

        assertThatThrownBy(() -> alert.assign(UserId.generate(), userId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void shouldResolve() {
        Alert alert = createAlert(AlertSeverity.WARNING);
        alert.clearDomainEvents();

        alert.resolve(userId, "Fixed the wiring");

        assertThat(alert.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.resolvedAt()).isEqualTo(now);
        assertThat(alert.resolvedBy()).isEqualTo(userId);
        assertThat(alert.resolutionNote()).isEqualTo("Fixed the wiring");
        assertThat(alert.getDomainEvents()).hasSize(1);
        AlertResolvedEvent event = (AlertResolvedEvent) alert.getDomainEvents().get(0);
        assertThat(event.falsePositive()).isFalse();
    }

    @Test
    void shouldMarkFalsePositive() {
        Alert alert = createAlert(AlertSeverity.INFO);
        alert.clearDomainEvents();

        alert.markFalsePositive(userId, "Sensor calibration issue");

        assertThat(alert.status()).isEqualTo(AlertStatus.FALSE_POSITIVE);
        assertThat(alert.resolutionNote()).isEqualTo("Sensor calibration issue");
        AlertResolvedEvent event = (AlertResolvedEvent) alert.getDomainEvents().get(0);
        assertThat(event.falsePositive()).isTrue();
    }

    @Test
    void shouldNotResolveAlreadyResolvedAlert() {
        Alert alert = createAlert(AlertSeverity.WARNING);
        alert.resolve(userId, "done");

        assertThatThrownBy(() -> alert.resolve(userId, "again"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void shouldEscalate() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);
        alert.clearDomainEvents();

        alert.escalate();

        assertThat(alert.escalationLevel()).isEqualTo(EscalationLevel.FIRST);
        assertThat(alert.lastEscalatedAt()).isEqualTo(now);
        assertThat(alert.getDomainEvents()).hasSize(1);
        assertThat(alert.getDomainEvents().get(0)).isInstanceOf(AlertEscalatedEvent.class);
    }

    @Test
    void shouldEscalateMultipleLevels() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);

        alert.escalate();
        alert.escalate();
        alert.escalate();

        assertThat(alert.escalationLevel()).isEqualTo(EscalationLevel.EMERGENCY);
    }

    @Test
    void shouldNotEscalateResolvedAlert() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);
        alert.resolve(userId, "done");
        alert.clearDomainEvents();

        alert.escalate();

        assertThat(alert.escalationLevel()).isEqualTo(EscalationLevel.NONE);
        assertThat(alert.getDomainEvents()).isEmpty();
    }

    @Test
    void shouldRecordOccurrence() {
        Alert alert = createAlert(AlertSeverity.WARNING);

        alert.recordOccurrence();
        alert.recordOccurrence();

        assertThat(alert.occurrenceCount()).isEqualTo(3);
        assertThat(alert.lastOccurrenceAt()).isEqualTo(now);
    }

    @Test
    void shouldAddComment() {
        Alert alert = createAlert(AlertSeverity.WARNING);

        alert.addComment(userId, "Investigating the issue");

        assertThat(alert.comments()).hasSize(1);
        assertThat(alert.comments().get(0).content()).isEqualTo("Investigating the issue");
        assertThat(alert.comments().get(0).author()).isEqualTo(userId);
    }

    @Test
    void shouldNotAddCommentToResolvedAlert() {
        Alert alert = createAlert(AlertSeverity.WARNING);
        alert.resolve(userId, "done");

        assertThatThrownBy(() -> alert.addComment(userId, "Too late"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void shouldDetectSlaBreached() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);
        ClockProvider.setClock(Clock.fixed(now.plus(Duration.ofHours(25)), ZoneId.of("UTC")));

        assertThat(alert.isSlaBreached()).isTrue();
    }

    @Test
    void shouldNotBeBreachedBeforeDeadline() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);

        assertThat(alert.isSlaBreached()).isFalse();
    }

    @Test
    void shouldNotBeBreachedIfResolved() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);
        alert.resolve(userId, "done");
        ClockProvider.setClock(Clock.fixed(now.plus(Duration.ofHours(25)), ZoneId.of("UTC")));

        assertThat(alert.isSlaBreached()).isFalse();
    }

    @Test
    void shouldEscalateWhenOverdue() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);
        ClockProvider.setClock(Clock.fixed(now.plus(Duration.ofHours(5)), ZoneId.of("UTC")));

        assertThat(alert.shouldEscalate(Duration.ofHours(4))).isTrue();
    }

    @Test
    void shouldNotEscalateNonCritical() {
        Alert alert = createAlert(AlertSeverity.WARNING);
        ClockProvider.setClock(Clock.fixed(now.plus(Duration.ofHours(5)), ZoneId.of("UTC")));

        assertThat(alert.shouldEscalate(Duration.ofHours(4))).isFalse();
    }

    @Test
    void shouldNotEscalateInProgressAlert() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);
        alert.assign(UserId.generate(), userId);
        ClockProvider.setClock(Clock.fixed(now.plus(Duration.ofHours(5)), ZoneId.of("UTC")));

        assertThat(alert.shouldEscalate(Duration.ofHours(4))).isFalse();
    }

    @Test
    void shouldComputeDeduplicationKey() {
        Alert alert = createAlert(AlertSeverity.CRITICAL);

        DeduplicationKey key = alert.deduplicationKey();
        assertThat(key.deviceId()).isEqualTo(deviceId);
        assertThat(key.type()).isEqualTo(AlertType.MICRO_ARC_DETECTED);
        assertThat(key.toStringKey()).contains(deviceId.value().toString());
    }

    @Test
    void shouldSetSlaBySecverity() {
        Alert critical = createAlert(AlertSeverity.CRITICAL);
        assertThat(critical.slaDeadline()).isEqualTo(now.plus(Duration.ofHours(24)));

        Alert warning = createAlert(AlertSeverity.WARNING);
        assertThat(warning.slaDeadline()).isEqualTo(now.plus(Duration.ofDays(7)));

        Alert info = createAlert(AlertSeverity.INFO);
        assertThat(info.slaDeadline()).isEqualTo(now.plus(Duration.ofDays(30)));
    }
}
