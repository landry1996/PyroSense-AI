package com.pyrosense.ingestion.adapter.in.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.ingestion.adapter.in.rest.dto.HeartbeatPayload;
import com.pyrosense.ingestion.adapter.in.rest.dto.IngestionResponse;
import com.pyrosense.ingestion.adapter.in.rest.dto.TelemetryPayload;
import com.pyrosense.ingestion.application.port.in.IngestHeartbeatUseCase;
import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase;
import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase.IngestionResult;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private final IngestTelemetryUseCase ingestTelemetry;
    private final IngestHeartbeatUseCase ingestHeartbeat;
    private final ObjectMapper objectMapper;
    private final Counter receivedCounter;
    private final Counter rejectedCounter;
    private final Counter heartbeatCounter;
    private final Timer latencyTimer;

    public IngestionController(IngestTelemetryUseCase ingestTelemetry,
                               IngestHeartbeatUseCase ingestHeartbeat,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.ingestTelemetry = ingestTelemetry;
        this.ingestHeartbeat = ingestHeartbeat;
        this.objectMapper = objectMapper;
        this.receivedCounter = Counter.builder("ingestion_received_total")
                .description("Total telemetry messages received")
                .register(meterRegistry);
        this.rejectedCounter = Counter.builder("ingestion_rejected_total")
                .description("Total telemetry messages rejected")
                .register(meterRegistry);
        this.heartbeatCounter = Counter.builder("device_heartbeat_total")
                .description("Total heartbeat messages received")
                .register(meterRegistry);
        this.latencyTimer = Timer.builder("ingestion_latency_ms")
                .description("Telemetry ingestion latency")
                .register(meterRegistry);
    }

    @PostMapping("/{tenantId}/{deviceId}/telemetry")
    public ResponseEntity<IngestionResponse> ingestTelemetry(
            @PathVariable UUID tenantId,
            @PathVariable String deviceId,
            @Valid @RequestBody TelemetryPayload payload) {

        receivedCounter.increment();

        return latencyTimer.record(() -> {
            String rawPayload;
            try {
                rawPayload = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                rejectedCounter.increment();
                return ResponseEntity.badRequest().body(IngestionResponse.rejected("Invalid payload"));
            }

            var command = new IngestTelemetryUseCase.TelemetryCommand(
                    DeviceId.from(deviceId),
                    TenantId.from(tenantId.toString()),
                    payload.timestamp(),
                    payload.samplingWindowMs(),
                    payload.rmsCurrent(),
                    payload.rmsVoltage(),
                    payload.activePower(),
                    payload.reactivePower(),
                    payload.powerFactor(),
                    payload.thd(),
                    payload.temperatureCelsius(),
                    payload.hfNoiseLevel(),
                    payload.microArcCount(),
                    payload.transientCount(),
                    payload.firmwareVersion(),
                    rawPayload
            );

            var result = ingestTelemetry.execute(command);
            return mapResult(result);
        });
    }

    @PostMapping("/{tenantId}/{deviceId}/heartbeat")
    public ResponseEntity<IngestionResponse> ingestHeartbeat(
            @PathVariable UUID tenantId,
            @PathVariable String deviceId,
            @Valid @RequestBody HeartbeatPayload payload) {

        heartbeatCounter.increment();

        var command = new IngestHeartbeatUseCase.HeartbeatCommand(
                DeviceId.from(deviceId),
                TenantId.from(tenantId.toString()),
                payload.timestamp(),
                payload.firmwareVersion(),
                payload.uptimeHours(),
                payload.memoryUsagePercent(),
                payload.cpuTemperatureCelsius()
        );

        ingestHeartbeat.execute(command);
        return ResponseEntity.ok(IngestionResponse.accepted());
    }

    private ResponseEntity<IngestionResponse> mapResult(IngestionResult result) {
        return switch (result) {
            case ACCEPTED -> ResponseEntity.status(HttpStatus.CREATED).body(IngestionResponse.accepted());
            case DUPLICATE -> ResponseEntity.ok(IngestionResponse.duplicate());
            case REJECTED_INVALID -> {
                rejectedCounter.increment();
                yield ResponseEntity.badRequest().body(IngestionResponse.rejected("Validation failed"));
            }
            case REJECTED_UNAUTHORIZED -> {
                rejectedCounter.increment();
                yield ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(IngestionResponse.rejected("Device not authorized"));
            }
        };
    }
}
