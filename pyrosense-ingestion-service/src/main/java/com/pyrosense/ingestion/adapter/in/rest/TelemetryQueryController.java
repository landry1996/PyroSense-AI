package com.pyrosense.ingestion.adapter.in.rest;

import com.pyrosense.ingestion.application.port.out.TelemetryQueryPort;
import com.pyrosense.ingestion.application.port.out.TelemetryQueryPort.AggregatedPoint;
import com.pyrosense.ingestion.application.port.out.TelemetryQueryPort.TimeSeriesPoint;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/telemetry")
@PreAuthorize("isAuthenticated()")
public class TelemetryQueryController {

    private final TelemetryQueryPort telemetryQuery;

    public TelemetryQueryController(TelemetryQueryPort telemetryQuery) {
        this.telemetryQuery = telemetryQuery;
    }

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<?> getDeviceTelemetry(
            @PathVariable String deviceId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1hour") String granularity,
            @RequestParam(defaultValue = "100") int limit) {

        TenantId tenantId = TenantContext.require();
        DeviceId device = new DeviceId(UUID.fromString(deviceId));

        Instant toTime = to != null ? Instant.parse(to) : Instant.now();
        Instant fromTime = from != null ? Instant.parse(from) : toTime.minus(24, ChronoUnit.HOURS);
        int safeLimit = Math.min(limit, 1000);

        return switch (granularity) {
            case "raw" -> {
                List<TimeSeriesPoint> points = telemetryQuery.findRawByDevice(
                        tenantId, device, fromTime, toTime, safeLimit);
                yield ResponseEntity.ok(points);
            }
            case "1min" -> {
                List<AggregatedPoint> points = telemetryQuery.findAggregated1Min(
                        tenantId, device, fromTime, toTime);
                yield ResponseEntity.ok(points);
            }
            case "15min" -> {
                List<AggregatedPoint> points = telemetryQuery.findAggregated15Min(
                        tenantId, device, fromTime, toTime);
                yield ResponseEntity.ok(points);
            }
            case "1hour" -> {
                List<AggregatedPoint> points = telemetryQuery.findAggregated1Hour(
                        tenantId, device, fromTime, toTime);
                yield ResponseEntity.ok(points);
            }
            case "daily" -> {
                List<AggregatedPoint> points = telemetryQuery.findAggregatedDaily(
                        tenantId, device, fromTime, toTime);
                yield ResponseEntity.ok(points);
            }
            default -> ResponseEntity.badRequest().body("Invalid granularity. Use: raw, 1min, 15min, 1hour, daily");
        };
    }

    @GetMapping("/devices/{deviceId}/count")
    public ResponseEntity<Long> getDeviceTelemetryCount(
            @PathVariable String deviceId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        TenantId tenantId = TenantContext.require();
        DeviceId device = new DeviceId(UUID.fromString(deviceId));

        Instant toTime = to != null ? Instant.parse(to) : Instant.now();
        Instant fromTime = from != null ? Instant.parse(from) : toTime.minus(24, ChronoUnit.HOURS);

        long count = telemetryQuery.countByDevice(tenantId, device, fromTime, toTime);
        return ResponseEntity.ok(count);
    }
}
