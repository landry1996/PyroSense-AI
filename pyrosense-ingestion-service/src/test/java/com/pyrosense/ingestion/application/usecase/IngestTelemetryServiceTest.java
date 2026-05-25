package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase;
import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase.IngestionResult;
import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase.TelemetryCommand;
import com.pyrosense.ingestion.application.port.out.DeviceAuthorizationPort;
import com.pyrosense.ingestion.application.port.out.IdempotencyPort;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.ingestion.application.port.out.TelemetryRepositoryPort;
import com.pyrosense.ingestion.domain.event.MicroArcDetectedEvent;
import com.pyrosense.ingestion.domain.event.TelemetryReceivedEvent;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestTelemetryServiceTest {

    @Mock
    private TelemetryRepositoryPort repository;
    @Mock
    private TelemetryEventPublisherPort eventPublisher;
    @Mock
    private DeviceAuthorizationPort deviceAuth;
    @Mock
    private IdempotencyPort idempotency;

    private IngestTelemetryService service;

    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC")));
        service = new IngestTelemetryService(repository, eventPublisher, deviceAuth, idempotency);
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    private TelemetryCommand validCommand() {
        return new TelemetryCommand(
                deviceId, tenantId, Instant.parse("2025-01-15T09:59:00Z"),
                1000, 12.4, 230.1, 2800.5, 300.2, 0.94, 4.7, 39.5, 0.18, 2, 1,
                "1.0.0", "{\"test\":true}");
    }

    @Test
    void shouldAcceptValidTelemetry() {
        when(deviceAuth.isDeviceActive(deviceId)).thenReturn(true);
        when(deviceAuth.isDeviceOwnedByTenant(deviceId, tenantId)).thenReturn(true);
        when(idempotency.isDuplicate(anyString())).thenReturn(false);

        var result = service.execute(validCommand());

        assertThat(result).isEqualTo(IngestionResult.ACCEPTED);
        verify(repository).save(any());
        verify(idempotency).markProcessed(anyString(), any());
    }

    @Test
    void shouldRejectInactiveDevice() {
        when(deviceAuth.isDeviceActive(deviceId)).thenReturn(false);

        var result = service.execute(validCommand());

        assertThat(result).isEqualTo(IngestionResult.REJECTED_UNAUTHORIZED);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectWrongTenant() {
        when(deviceAuth.isDeviceActive(deviceId)).thenReturn(true);
        when(deviceAuth.isDeviceOwnedByTenant(deviceId, tenantId)).thenReturn(false);

        var result = service.execute(validCommand());

        assertThat(result).isEqualTo(IngestionResult.REJECTED_UNAUTHORIZED);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidData() {
        when(deviceAuth.isDeviceActive(deviceId)).thenReturn(true);
        when(deviceAuth.isDeviceOwnedByTenant(deviceId, tenantId)).thenReturn(true);

        var command = new TelemetryCommand(
                deviceId, tenantId, Instant.parse("2025-01-15T09:59:00Z"),
                1000, -5.0, 230.1, 2800.5, 300.2, 0.94, 4.7, 39.5, 0.18, 0, 0,
                "1.0.0", "{\"test\":true}");

        var result = service.execute(command);

        assertThat(result).isEqualTo(IngestionResult.REJECTED_INVALID);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldDetectDuplicate() {
        when(deviceAuth.isDeviceActive(deviceId)).thenReturn(true);
        when(deviceAuth.isDeviceOwnedByTenant(deviceId, tenantId)).thenReturn(true);
        when(idempotency.isDuplicate(anyString())).thenReturn(true);

        var result = service.execute(validCommand());

        assertThat(result).isEqualTo(IngestionResult.DUPLICATE);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldPublishTelemetryReceivedEvent() {
        when(deviceAuth.isDeviceActive(deviceId)).thenReturn(true);
        when(deviceAuth.isDeviceOwnedByTenant(deviceId, tenantId)).thenReturn(true);
        when(idempotency.isDuplicate(anyString())).thenReturn(false);

        service.execute(validCommand());

        var captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, atLeast(1)).publish(captor.capture());

        var events = captor.getAllValues();
        assertThat(events).anyMatch(e -> e instanceof TelemetryReceivedEvent);
    }

    @Test
    void shouldPublishMicroArcEventWhenDetected() {
        when(deviceAuth.isDeviceActive(deviceId)).thenReturn(true);
        when(deviceAuth.isDeviceOwnedByTenant(deviceId, tenantId)).thenReturn(true);
        when(idempotency.isDuplicate(anyString())).thenReturn(false);

        service.execute(validCommand());

        var captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publish(captor.capture());

        var events = captor.getAllValues();
        assertThat(events).anyMatch(e -> e instanceof MicroArcDetectedEvent);
    }

    @Test
    void shouldNotPublishMicroArcEventWhenNoArcs() {
        when(deviceAuth.isDeviceActive(deviceId)).thenReturn(true);
        when(deviceAuth.isDeviceOwnedByTenant(deviceId, tenantId)).thenReturn(true);
        when(idempotency.isDuplicate(anyString())).thenReturn(false);

        var command = new TelemetryCommand(
                deviceId, tenantId, Instant.parse("2025-01-15T09:59:00Z"),
                1000, 12.4, 230.1, 2800.5, 300.2, 0.94, 4.7, 39.5, 0.18, 0, 0,
                "1.0.0", "{\"test\":true}");

        service.execute(command);

        var captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(1)).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TelemetryReceivedEvent.class);
    }
}
