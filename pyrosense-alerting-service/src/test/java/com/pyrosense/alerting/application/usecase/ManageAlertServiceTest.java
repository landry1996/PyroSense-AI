package com.pyrosense.alerting.application.usecase;

import com.pyrosense.alerting.application.port.out.AlertEventPublisherPort;
import com.pyrosense.alerting.application.port.out.AlertRepositoryPort;
import com.pyrosense.alerting.domain.model.*;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.exception.InvalidStateTransitionException;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageAlertServiceTest {

    @Mock private AlertRepositoryPort repository;
    @Mock private AlertEventPublisherPort eventPublisher;

    private ManageAlertService service;
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final UserId userId = UserId.generate();

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
        service = new ManageAlertService(repository, eventPublisher);
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    private Alert createTestAlert() {
        Alert alert = Alert.create(TenantId.generate(), DeviceId.generate(), AlertType.OVERHEATING,
                AlertSeverity.CRITICAL, "Test", "Desc", SlaPolicy.defaults());
        alert.clearDomainEvents();
        return alert;
    }

    @Test
    void shouldAcknowledgeAlert() {
        Alert alert = createTestAlert();
        when(repository.findById(alert.getId())).thenReturn(Optional.of(alert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Alert result = service.acknowledge(alert.getId(), userId);

        assertThat(result.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void shouldThrowWhenAlertNotFound() {
        AlertId id = AlertId.generate();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acknowledge(id, userId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldAssignAlert() {
        Alert alert = createTestAlert();
        UserId assignee = UserId.generate();
        when(repository.findById(alert.getId())).thenReturn(Optional.of(alert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Alert result = service.assign(alert.getId(), assignee, userId);

        assertThat(result.status()).isEqualTo(AlertStatus.IN_PROGRESS);
        assertThat(result.assignedTo()).isEqualTo(assignee);
    }

    @Test
    void shouldResolveAlert() {
        Alert alert = createTestAlert();
        when(repository.findById(alert.getId())).thenReturn(Optional.of(alert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Alert result = service.resolve(alert.getId(), userId, "Wiring replaced");

        assertThat(result.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(result.resolutionNote()).isEqualTo("Wiring replaced");
    }

    @Test
    void shouldMarkFalsePositive() {
        Alert alert = createTestAlert();
        when(repository.findById(alert.getId())).thenReturn(Optional.of(alert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Alert result = service.markFalsePositive(alert.getId(), userId, "Sensor defect");

        assertThat(result.status()).isEqualTo(AlertStatus.FALSE_POSITIVE);
    }

    @Test
    void shouldAddComment() {
        Alert alert = createTestAlert();
        when(repository.findById(alert.getId())).thenReturn(Optional.of(alert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Alert result = service.addComment(alert.getId(), userId, "Checking on site");

        assertThat(result.comments()).hasSize(1);
        assertThat(result.comments().get(0).content()).isEqualTo("Checking on site");
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectInvalidTransition() {
        Alert alert = createTestAlert();
        alert.resolve(userId, "done");
        alert.clearDomainEvents();
        when(repository.findById(alert.getId())).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> service.acknowledge(alert.getId(), userId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
