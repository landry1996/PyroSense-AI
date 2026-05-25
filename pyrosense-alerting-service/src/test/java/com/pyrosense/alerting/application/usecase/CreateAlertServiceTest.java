package com.pyrosense.alerting.application.usecase;

import com.pyrosense.alerting.application.port.in.CreateAlertUseCase.CreateAlertCommand;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateAlertServiceTest {

    @Mock private AlertRepositoryPort repository;
    @Mock private AlertEventPublisherPort eventPublisher;

    private CreateAlertService service;
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final TenantId tenantId = TenantId.generate();
    private final DeviceId deviceId = DeviceId.generate();

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
        service = new CreateAlertService(repository, eventPublisher, SlaPolicy.defaults());
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldCreateNewAlert() {
        when(repository.findActiveByDeduplicationKey(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new CreateAlertCommand(tenantId, deviceId, AlertType.OVERHEATING,
                AlertSeverity.CRITICAL, "Surchauffe", "Température critique");
        Alert result = service.create(command);

        assertThat(result.status()).isEqualTo(AlertStatus.OPEN);
        assertThat(result.severity()).isEqualTo(AlertSeverity.CRITICAL);
        verify(repository).save(any(Alert.class));
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void shouldDeduplicateExistingAlert() {
        Alert existing = Alert.create(tenantId, deviceId, AlertType.OVERHEATING,
                AlertSeverity.CRITICAL, "Existing", "Desc", SlaPolicy.defaults());
        existing.clearDomainEvents();
        when(repository.findActiveByDeduplicationKey(any())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new CreateAlertCommand(tenantId, deviceId, AlertType.OVERHEATING,
                AlertSeverity.CRITICAL, "Duplicate", "Desc");
        Alert result = service.create(command);

        assertThat(result.occurrenceCount()).isEqualTo(2);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldPublishCreatedEvent() {
        when(repository.findActiveByDeduplicationKey(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new CreateAlertCommand(tenantId, deviceId, AlertType.MICRO_ARC_DETECTED,
                AlertSeverity.CRITICAL, "Arc", "Detected");
        service.create(command);

        var captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("alerting.alert.created");
    }
}
