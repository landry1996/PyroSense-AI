package com.pyrosense.ingestion.adapter.in.rest;

import com.pyrosense.ingestion.application.port.out.DeviceCapabilityLookupPort;
import com.pyrosense.ingestion.application.port.out.RejectionRepositoryPort;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices/quality")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'DEVICE_MANAGER')")
public class DeviceQualityController {

    private static final Logger log = LoggerFactory.getLogger(DeviceQualityController.class);

    private final MeterRegistry meterRegistry;
    private final DeviceCapabilityLookupPort capabilityLookup;
    private final StringRedisTemplate redisTemplate;

    public DeviceQualityController(MeterRegistry meterRegistry,
                                    DeviceCapabilityLookupPort capabilityLookup,
                                    StringRedisTemplate redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.capabilityLookup = capabilityLookup;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<QualitySummaryResponse> getQualitySummary() {
        TenantContext.require();

        // WARNING: These metrics are platform-wide (not filtered by tenant).
        // This endpoint is restricted to PLATFORM_ADMIN role only.
        log.debug("Returning platform-wide quality metrics summary (not tenant-scoped)");

        double totalReceived = getCounterValue("real_device_telemetry_received_total");
        double signatureInvalid = getCounterValue("telemetry_signature_invalid_total");
        double replayDetected = getCounterValue("telemetry_replay_detected_total");
        double lowSignalQuality = getCounterValue("low_signal_quality_total");
        double clockDrift = getCounterValue("device_clock_drift_total");
        double totalRejected = getCounterValue("pyrosense.telemetry.rejected");

        double acceptanceRate = totalReceived > 0 ?
                (totalReceived / (totalReceived + totalRejected)) * 100 : 100.0;

        return ResponseEntity.ok(new QualitySummaryResponse(
                (long) totalReceived,
                (long) totalRejected,
                (long) signatureInvalid,
                (long) replayDetected,
                (long) lowSignalQuality,
                (long) clockDrift,
                Math.round(acceptanceRate * 100.0) / 100.0
        ));
    }

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<DeviceQualityResponse> getDeviceQuality(@PathVariable UUID deviceId) {
        TenantContext.require();
        String deviceIdStr = deviceId.toString();

        var capability = capabilityLookup.getCapability(deviceIdStr);
        String lastSequence = redisTemplate.opsForValue().get("ingestion:seq:" + deviceIdStr);

        return ResponseEntity.ok(new DeviceQualityResponse(
                deviceIdStr,
                capability.map(c -> c.firmwareVersion()).orElse("unknown"),
                capability.map(c -> c.supportsSignature()).orElse(false),
                capability.map(c -> c.supportsAntiReplay()).orElse(false),
                lastSequence != null ? Long.parseLong(lastSequence) : 0,
                capability.isPresent()
        ));
    }

    private double getCounterValue(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter != null ? counter.count() : 0.0;
    }

    public record QualitySummaryResponse(
            long totalRealDeviceMessages,
            long totalRejected,
            long signatureInvalid,
            long replayDetected,
            long lowSignalQuality,
            long clockDrift,
            double acceptanceRatePercent
    ) {}

    public record DeviceQualityResponse(
            String deviceId,
            String firmwareVersion,
            boolean supportsSignature,
            boolean supportsAntiReplay,
            long lastSequenceNumber,
            boolean isRealDevice
    ) {}
}
