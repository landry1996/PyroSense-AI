package com.pyrosense.dashboard.adapter.in.rest;

import com.pyrosense.dashboard.application.port.in.DeviceTechnicalQuery;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.dashboard.domain.model.PilotDashboard.IncidentsSummary;
import com.pyrosense.dashboard.domain.model.PilotDashboard.PilotDeviceSummary;
import com.pyrosense.dashboard.domain.model.TelemetryQualityReport.*;
import com.pyrosense.shared.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DeviceTechnicalController {

    private final DeviceTechnicalQuery deviceTechnicalQuery;

    public DeviceTechnicalController(DeviceTechnicalQuery deviceTechnicalQuery) {
        this.deviceTechnicalQuery = deviceTechnicalQuery;
    }

    @GetMapping("/devices/{id}/technical-health")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'SUPPORT_READONLY')")
    public ResponseEntity<TechnicalHealthResponse> getTechnicalHealth(@PathVariable String id) {
        String tenantId = TenantContext.require().value().toString();
        DeviceTechnicalHealth health = deviceTechnicalQuery.getDeviceTechnicalHealth(id, tenantId);
        return ResponseEntity.ok(toTechnicalHealthResponse(health));
    }

    @GetMapping("/devices/{id}/telemetry-quality")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'SUPPORT_READONLY')")
    public ResponseEntity<TelemetryQualityResponse> getTelemetryQuality(
            @PathVariable String id,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        String tenantId = TenantContext.require().value().toString();
        TelemetryQualityReport report = deviceTechnicalQuery.getTelemetryQuality(id, tenantId, from, to);
        return ResponseEntity.ok(toTelemetryQualityResponse(report));
    }

    @GetMapping("/devices/{id}/security-status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'SUPPORT_READONLY')")
    public ResponseEntity<SecurityStatusResponse> getSecurityStatus(@PathVariable String id) {
        String tenantId = TenantContext.require().value().toString();
        DeviceSecurityStatus status = deviceTechnicalQuery.getDeviceSecurityStatus(id, tenantId);
        return ResponseEntity.ok(toSecurityStatusResponse(status));
    }

    @GetMapping("/pilots/{id}/dashboard")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'SUPPORT_READONLY')")
    public ResponseEntity<PilotDashboardResponse> getPilotDashboard(@PathVariable UUID id) {
        String tenantId = TenantContext.require().value().toString();
        PilotDashboard dashboard = deviceTechnicalQuery.getPilotDashboard(id, tenantId);
        return ResponseEntity.ok(toPilotDashboardResponse(dashboard));
    }

    // --- Response DTOs ---

    record TechnicalHealthResponse(
            String deviceId,
            String serialNumber,
            String firmwareVersion,
            String hardwareRevision,
            String connectivity,
            Instant lastHeartbeat,
            long uptimeSeconds,
            double signalQuality,
            double dataQualityScore,
            String dataQualityGrade,
            Double batteryPercent,
            Double deviceTemperature,
            long clockDriftMs,
            int sequenceGaps,
            int rejectedTelemetryCount,
            String status
    ) {}

    record TelemetryQualityResponse(
            String deviceId,
            List<HourlyCountDto> receivedPerHour,
            List<HourlyCountDto> rejectedPerHour,
            List<RejectionReasonDto> rejectionReasons,
            List<QualityPointDto> signalQualityTrend,
            List<OfflinePeriodDto> offlinePeriods
    ) {}

    record HourlyCountDto(Instant hour, int count) {}
    record RejectionReasonDto(String reason, int count) {}
    record QualityPointDto(Instant timestamp, double quality) {}
    record OfflinePeriodDto(Instant start, Instant end, long durationMinutes) {}

    record SecurityStatusResponse(
            String deviceId,
            String credentialStatus,
            int credentialVersion,
            Instant lastRotation,
            int failedAuthAttempts,
            int replayAttemptsBlocked,
            boolean isRevoked,
            Instant lastAuthFailure
    ) {}

    record PilotDashboardResponse(
            UUID pilotId,
            String pilotName,
            String status,
            List<PilotDeviceSummaryDto> devices,
            IncidentsSummaryDto incidentsSummary,
            PilotKpiSnapshotDto kpiSnapshot
    ) {}

    record PilotDeviceSummaryDto(
            String deviceId,
            String serialNumber,
            String status,
            double signalQuality,
            String dataQualityGrade,
            Instant lastHeartbeat,
            String installationStatus
    ) {}

    record IncidentsSummaryDto(int open, int investigating, int resolved, int total) {}

    record PilotKpiSnapshotDto(
            UUID id,
            LocalDate date,
            int totalDevices,
            int activeDevices,
            double uptimePercent,
            double telemetryValidPercent,
            double avgSignalQuality,
            int alertsGenerated,
            int alertsConfirmed,
            int falsePositives,
            double falsePositiveRate,
            int incidentsOpen,
            int incidentsResolved,
            Instant computedAt
    ) {}

    // --- Mappers ---

    private TechnicalHealthResponse toTechnicalHealthResponse(DeviceTechnicalHealth h) {
        return new TechnicalHealthResponse(
                h.deviceId(), h.serialNumber(), h.firmwareVersion(), h.hardwareRevision(),
                h.connectivity(), h.lastHeartbeat(), h.uptimeSeconds(), h.signalQuality(),
                h.dataQualityScore(), h.dataQualityGrade(), h.batteryPercent(),
                h.deviceTemperature(), h.clockDriftMs(), h.sequenceGaps(),
                h.rejectedTelemetryCount(), h.status()
        );
    }

    private TelemetryQualityResponse toTelemetryQualityResponse(TelemetryQualityReport r) {
        return new TelemetryQualityResponse(
                r.deviceId(),
                r.receivedPerHour().stream()
                        .map(h -> new HourlyCountDto(h.hour(), h.count())).toList(),
                r.rejectedPerHour().stream()
                        .map(h -> new HourlyCountDto(h.hour(), h.count())).toList(),
                r.rejectionReasons().stream()
                        .map(rr -> new RejectionReasonDto(rr.reason(), rr.count())).toList(),
                r.signalQualityTrend().stream()
                        .map(q -> new QualityPointDto(q.timestamp(), q.quality())).toList(),
                r.offlinePeriods().stream()
                        .map(o -> new OfflinePeriodDto(o.start(), o.end(), o.durationMinutes())).toList()
        );
    }

    private SecurityStatusResponse toSecurityStatusResponse(DeviceSecurityStatus s) {
        return new SecurityStatusResponse(
                s.deviceId(), s.credentialStatus(), s.credentialVersion(),
                s.lastRotation(), s.failedAuthAttempts(), s.replayAttemptsBlocked(),
                s.isRevoked(), s.lastAuthFailure()
        );
    }

    private PilotDashboardResponse toPilotDashboardResponse(PilotDashboard d) {
        List<PilotDeviceSummaryDto> devices = d.devices().stream()
                .map(dev -> new PilotDeviceSummaryDto(
                        dev.deviceId(), dev.serialNumber(), dev.status(),
                        dev.signalQuality(), dev.dataQualityGrade(),
                        dev.lastHeartbeat(), dev.installationStatus()
                )).toList();

        IncidentsSummaryDto incidents = new IncidentsSummaryDto(
                d.incidentsSummary().open(), d.incidentsSummary().investigating(),
                d.incidentsSummary().resolved(), d.incidentsSummary().total()
        );

        PilotKpiSnapshotDto kpi = d.kpiSnapshot() == null ? null :
                new PilotKpiSnapshotDto(
                        d.kpiSnapshot().id(), d.kpiSnapshot().date(),
                        d.kpiSnapshot().totalDevices(), d.kpiSnapshot().activeDevices(),
                        d.kpiSnapshot().uptimePercent(), d.kpiSnapshot().telemetryValidPercent(),
                        d.kpiSnapshot().avgSignalQuality(), d.kpiSnapshot().alertsGenerated(),
                        d.kpiSnapshot().alertsConfirmed(), d.kpiSnapshot().falsePositives(),
                        d.kpiSnapshot().falsePositiveRate(), d.kpiSnapshot().incidentsOpen(),
                        d.kpiSnapshot().incidentsResolved(), d.kpiSnapshot().computedAt()
                );

        return new PilotDashboardResponse(
                d.pilotId(), d.pilotName(), d.status(), devices, incidents, kpi
        );
    }
}
