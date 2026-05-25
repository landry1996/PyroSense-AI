package com.pyrosense.ingestion.performance;

import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase.IngestionResult;
import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase.TelemetryCommand;
import com.pyrosense.ingestion.application.port.out.DeviceAuthorizationPort;
import com.pyrosense.ingestion.application.port.out.IdempotencyPort;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.ingestion.application.port.out.TelemetryRepositoryPort;
import com.pyrosense.ingestion.application.usecase.IngestTelemetryService;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionPerformanceSmokeTest {

    private static final int MESSAGE_COUNT = 1000;
    private static final long MAX_AVG_MS = 10;
    private static final long MAX_TOTAL_MS = 5000;

    @Mock private TelemetryRepositoryPort repository;
    @Mock private TelemetryEventPublisherPort eventPublisher;
    @Mock private DeviceAuthorizationPort deviceAuth;
    @Mock private IdempotencyPort idempotency;

    private IngestTelemetryService service;
    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant fixedNow = Instant.parse("2025-06-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(fixedNow, ZoneId.of("UTC")));
        service = new IngestTelemetryService(repository, eventPublisher, deviceAuth, idempotency);

        when(deviceAuth.isDeviceActive(any())).thenReturn(true);
        when(deviceAuth.isDeviceOwnedByTenant(any(), any())).thenReturn(true);
        when(idempotency.isDuplicate(anyString())).thenReturn(false);
        doNothing().when(repository).save(any());
        doNothing().when(idempotency).markProcessed(anyString(), any());
        doNothing().when(eventPublisher).publish(any());
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldProcess1000MessagesWithAcceptableLatency() {
        List<TelemetryCommand> commands = buildCommands(MESSAGE_COUNT);

        // Warm up JIT
        for (int i = 0; i < 50; i++) {
            service.execute(commands.get(i));
        }

        long startNanos = System.nanoTime();

        for (TelemetryCommand command : commands) {
            IngestionResult result = service.execute(command);
            assertThat(result).isEqualTo(IngestionResult.ACCEPTED);
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        long elapsedMs = elapsedNanos / 1_000_000;
        double avgMs = (double) elapsedNanos / MESSAGE_COUNT / 1_000_000.0;

        assertThat(elapsedMs)
                .as("Total processing time for %d messages should be under %d ms, was %d ms",
                        MESSAGE_COUNT, MAX_TOTAL_MS, elapsedMs)
                .isLessThan(MAX_TOTAL_MS);

        assertThat(avgMs)
                .as("Average processing time per message should be under %d ms, was %.2f ms",
                        MAX_AVG_MS, avgMs)
                .isLessThan((double) MAX_AVG_MS);
    }

    private List<TelemetryCommand> buildCommands(int count) {
        List<TelemetryCommand> commands = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            commands.add(new TelemetryCommand(
                    deviceId,
                    tenantId,
                    fixedNow.minusSeconds(count - i),
                    1000,
                    12.4 + (i % 10) * 0.1,
                    230.0 + (i % 5) * 0.2,
                    2800.0 + i,
                    300.0 + (i % 20),
                    0.92 + (i % 8) * 0.01,
                    4.5 + (i % 15) * 0.1,
                    38.0 + (i % 12) * 0.5,
                    0.15 + (i % 10) * 0.01,
                    i % 5,
                    i % 3,
                    "1.0.0",
                    "{\"seq\":" + i + ",\"data\":\"payload-" + i + "\"}"
            ));
        }
        return commands;
    }
}
