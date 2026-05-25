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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertDeduplicationTest {

    @Mock private AlertRepositoryPort repository;
    @Mock private AlertEventPublisherPort eventPublisher;

    private CreateAlertService service;
    private final Instant now = Instant.parse("2025-03-10T14:00:00Z");
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
    void firstAlertWithDeduplicationKeyShouldBeCreatedNormally() {
        DeduplicationKey key = new DeduplicationKey(deviceId, AlertType.OVERHEATING);
        when(repository.findActiveByDeduplicationKey(key)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new CreateAlertCommand(tenantId, deviceId, AlertType.OVERHEATING,
                AlertSeverity.CRITICAL, "Overheating detected", "Temperature exceeds threshold");
        Alert result = service.create(command);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(AlertStatus.OPEN);
        assertThat(result.occurrenceCount()).isEqualTo(1);
        assertThat(result.deduplicationKey()).isEqualTo(key);
        verify(repository).save(any(Alert.class));
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void secondAlertWithSameDeduplicationKeyWithinWindowShouldBeDeduplicated() {
        DeduplicationKey key = new DeduplicationKey(deviceId, AlertType.OVERHEATING);

        Alert existingAlert = Alert.create(tenantId, deviceId, AlertType.OVERHEATING,
                AlertSeverity.CRITICAL, "Existing alert", "Already active", SlaPolicy.defaults());
        existingAlert.clearDomainEvents();

        when(repository.findActiveByDeduplicationKey(key)).thenReturn(Optional.of(existingAlert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new CreateAlertCommand(tenantId, deviceId, AlertType.OVERHEATING,
                AlertSeverity.CRITICAL, "Duplicate alert", "Same device and type");
        Alert result = service.create(command);

        assertThat(result.occurrenceCount()).isEqualTo(2);
        assertThat(result.status()).isEqualTo(AlertStatus.OPEN);
        verify(eventPublisher, never()).publish(any());
        verify(repository).save(existingAlert);
    }

    @Test
    void alertWithDifferentDeduplicationKeyShouldBeCreatedNormally() {
        DeviceId otherDeviceId = DeviceId.generate();
        DeduplicationKey keyDifferent = new DeduplicationKey(otherDeviceId, AlertType.MICRO_ARC_DETECTED);

        when(repository.findActiveByDeduplicationKey(keyDifferent)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new CreateAlertCommand(tenantId, otherDeviceId, AlertType.MICRO_ARC_DETECTED,
                AlertSeverity.CRITICAL, "Arc fault", "Different device and type");
        Alert result = service.create(command);

        assertThat(result.occurrenceCount()).isEqualTo(1);
        assertThat(result.deduplicationKey()).isEqualTo(keyDifferent);
        assertThat(result.status()).isEqualTo(AlertStatus.OPEN);
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void afterDeduplicationWindowExpiresAlertWithSameKeyShouldBeCreatedAsNew() {
        DeduplicationKey key = new DeduplicationKey(deviceId, AlertType.OVERHEATING);

        // Simulate that no active alert exists for this key (window expired / alert was resolved)
        when(repository.findActiveByDeduplicationKey(key)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new CreateAlertCommand(tenantId, deviceId, AlertType.OVERHEATING,
                AlertSeverity.CRITICAL, "New alert after expiry", "Window expired");
        Alert result = service.create(command);

        assertThat(result.occurrenceCount()).isEqualTo(1);
        assertThat(result.status()).isEqualTo(AlertStatus.OPEN);
        assertThat(result.deduplicationKey()).isEqualTo(key);
        verify(repository).save(any(Alert.class));
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void multipleDeduplicationsOnSameAlertShouldIncrementOccurrenceCount() {
        DeduplicationKey key = new DeduplicationKey(deviceId, AlertType.OVERHEATING);

        Alert existingAlert = Alert.create(tenantId, deviceId, AlertType.OVERHEATING,
                AlertSeverity.CRITICAL, "Existing", "Active", SlaPolicy.defaults());
        existingAlert.clearDomainEvents();
        // Simulate already having 3 occurrences
        existingAlert.recordOccurrence();
        existingAlert.recordOccurrence();

        when(repository.findActiveByDeduplicationKey(key)).thenReturn(Optional.of(existingAlert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new CreateAlertCommand(tenantId, deviceId, AlertType.OVERHEATING,
                AlertSeverity.CRITICAL, "Another duplicate", "Fourth occurrence");
        Alert result = service.create(command);

        assertThat(result.occurrenceCount()).isEqualTo(4);
        verify(eventPublisher, never()).publish(any());
    }
}
