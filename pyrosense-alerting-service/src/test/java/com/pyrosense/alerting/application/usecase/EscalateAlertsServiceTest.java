package com.pyrosense.alerting.application.usecase;

import com.pyrosense.alerting.application.port.out.AlertEventPublisherPort;
import com.pyrosense.alerting.application.port.out.AlertRepositoryPort;
import com.pyrosense.alerting.domain.model.*;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscalateAlertsServiceTest {

    @Mock private AlertRepositoryPort repository;
    @Mock private AlertEventPublisherPort eventPublisher;

    private EscalateAlertsService service;
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
        service = new EscalateAlertsService(repository, eventPublisher, SlaPolicy.defaults());
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldEscalateOverdueAlerts() {
        Instant createdLongAgo = now.minus(Duration.ofHours(5));
        ClockProvider.setClock(Clock.fixed(createdLongAgo, ZoneId.of("UTC")));
        Alert alert = Alert.create(TenantId.generate(), DeviceId.generate(), AlertType.MICRO_ARC_DETECTED,
                AlertSeverity.CRITICAL, "Old", "Created 5h ago", SlaPolicy.defaults());
        alert.clearDomainEvents();
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));

        when(repository.findEscalationCandidates()).thenReturn(List.of(alert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.escalateOverdueAlerts();

        assertThat(count).isEqualTo(1);
        assertThat(alert.escalationLevel()).isEqualTo(EscalationLevel.FIRST);
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void shouldNotEscalateRecentAlerts() {
        Alert alert = Alert.create(TenantId.generate(), DeviceId.generate(), AlertType.MICRO_ARC_DETECTED,
                AlertSeverity.CRITICAL, "Recent", "Just created", SlaPolicy.defaults());
        alert.clearDomainEvents();

        when(repository.findEscalationCandidates()).thenReturn(List.of(alert));

        int count = service.escalateOverdueAlerts();

        assertThat(count).isEqualTo(0);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldReturnZeroWhenNoCandidates() {
        when(repository.findEscalationCandidates()).thenReturn(List.of());

        int count = service.escalateOverdueAlerts();

        assertThat(count).isEqualTo(0);
    }
}
