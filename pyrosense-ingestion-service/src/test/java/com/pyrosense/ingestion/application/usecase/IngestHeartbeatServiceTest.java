package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.IngestHeartbeatUseCase.HeartbeatCommand;
import com.pyrosense.ingestion.application.port.out.DeviceAuthorizationPort;
import com.pyrosense.ingestion.application.port.out.HeartbeatRepositoryPort;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.ingestion.domain.event.HeartbeatReceivedEvent;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestHeartbeatServiceTest {

    @Mock
    private DeviceAuthorizationPort deviceAuth;
    @Mock
    private TelemetryEventPublisherPort eventPublisher;
    @Mock
    private HeartbeatRepositoryPort heartbeatRepository;

    private IngestHeartbeatService service;
    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC")));
        service = new IngestHeartbeatService(deviceAuth, eventPublisher, heartbeatRepository);
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldPublishHeartbeatEventForActiveDevice() {
        when(deviceAuth.isDeviceActive(deviceId)).thenReturn(true);

        var command = new HeartbeatCommand(
                deviceId, tenantId, Instant.now(), "1.0.0", 100.5, 45.0, 38.0);

        service.execute(command);

        verify(heartbeatRepository).save(any());
        var captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(HeartbeatReceivedEvent.class);
        var event = (HeartbeatReceivedEvent) captor.getValue();
        assertThat(event.deviceId()).isEqualTo(deviceId);
        assertThat(event.firmwareVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldIgnoreHeartbeatForInactiveDevice() {
        when(deviceAuth.isDeviceActive(deviceId)).thenReturn(false);

        var command = new HeartbeatCommand(
                deviceId, tenantId, Instant.now(), "1.0.0", 100.5, 45.0, 38.0);

        service.execute(command);

        verify(eventPublisher, never()).publish(any());
        verify(heartbeatRepository, never()).save(any());
    }
}
