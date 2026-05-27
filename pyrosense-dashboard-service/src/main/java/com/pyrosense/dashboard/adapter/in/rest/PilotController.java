package com.pyrosense.dashboard.adapter.in.rest;

import com.pyrosense.dashboard.application.port.in.ManagePilotUseCase;
import com.pyrosense.dashboard.application.port.in.ManagePilotUseCase.*;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.shared.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pilots")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'PROPERTY_MANAGER', 'PLATFORM_ADMIN')")
public class PilotController {

    private final ManagePilotUseCase pilotUseCase;

    public PilotController(ManagePilotUseCase pilotUseCase) {
        this.pilotUseCase = pilotUseCase;
    }

    @PostMapping
    public ResponseEntity<PilotResponse> createPilot(@RequestBody CreatePilotRequest request) {
        String tenantId = TenantContext.require().value().toString();
        var command = new CreatePilotCommand(tenantId, request.name(), request.description());
        PilotProgram pilot = pilotUseCase.createPilot(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(pilot));
    }

    @GetMapping
    public ResponseEntity<List<PilotResponse>> listPilots() {
        String tenantId = TenantContext.require().value().toString();
        List<PilotResponse> pilots = pilotUseCase.listPilots(tenantId).stream()
                .map(this::toResponse).toList();
        return ResponseEntity.ok(pilots);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PilotDetailResponse> getPilot(@PathVariable UUID id) {
        String tenantId = TenantContext.require().value().toString();
        PilotProgram pilot = pilotUseCase.getPilot(id, tenantId);
        return ResponseEntity.ok(toDetailResponse(pilot));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PilotResponse> updateStatus(@PathVariable UUID id,
                                                       @RequestBody UpdateStatusRequest request) {
        String tenantId = TenantContext.require().value().toString();
        PilotProgram pilot = pilotUseCase.updateStatus(id, tenantId, request.status());
        return ResponseEntity.ok(toResponse(pilot));
    }

    @PostMapping("/{id}/devices")
    public ResponseEntity<PilotDeviceResponse> addDevice(@PathVariable UUID id,
                                                          @RequestBody AddDeviceRequest request) {
        String tenantId = TenantContext.require().value().toString();
        var command = new AddDeviceCommand(request.deviceId(), request.serialNumber(),
                request.siteName(), request.circuitDescription());
        PilotDevice device = pilotUseCase.addDevice(id, tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDeviceResponse(device));
    }

    @PostMapping("/{id}/observations")
    public ResponseEntity<PilotObservationResponse> addObservation(@PathVariable UUID id,
                                                                     @RequestBody AddObservationRequest request) {
        String tenantId = TenantContext.require().value().toString();
        var command = new AddObservationCommand(request.authorId(), request.authorName(),
                request.type(), request.content(), request.deviceId(), request.siteName());
        PilotObservation obs = pilotUseCase.addObservation(id, tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toObservationResponse(obs));
    }

    @PostMapping("/{id}/incidents")
    public ResponseEntity<PilotIncidentResponse> reportIncident(@PathVariable UUID id,
                                                                  @RequestBody ReportIncidentRequest request) {
        String tenantId = TenantContext.require().value().toString();
        var command = new ReportIncidentCommand(request.reportedBy(), request.severity(),
                request.category(), request.title(), request.description(),
                request.deviceId(), request.siteName());
        PilotIncident incident = pilotUseCase.reportIncident(id, tenantId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toIncidentResponse(incident));
    }

    @GetMapping("/{id}/kpis")
    public ResponseEntity<List<PilotKpiResponse>> getKpis(@PathVariable UUID id) {
        String tenantId = TenantContext.require().value().toString();
        List<PilotKpiResponse> kpis = pilotUseCase.getKpis(id, tenantId).stream()
                .map(this::toKpiResponse).toList();
        return ResponseEntity.ok(kpis);
    }

    @PostMapping("/{id}/reports")
    public ResponseEntity<PilotKpiResponse> generateReport(@PathVariable UUID id) {
        String tenantId = TenantContext.require().value().toString();
        PilotKpiSnapshot snapshot = pilotUseCase.computeCurrentKpis(id, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toKpiResponse(snapshot));
    }

    // --- Request DTOs ---

    record CreatePilotRequest(String name, String description) {}
    record UpdateStatusRequest(PilotStatus status) {}
    record AddDeviceRequest(String deviceId, String serialNumber, String siteName, String circuitDescription) {}
    record AddObservationRequest(String authorId, String authorName,
                                  PilotObservation.ObservationType type, String content,
                                  String deviceId, String siteName) {}
    record ReportIncidentRequest(String reportedBy, PilotIncident.IncidentSeverity severity,
                                  PilotIncident.IncidentCategory category, String title,
                                  String description, String deviceId, String siteName) {}

    // --- Response DTOs ---

    record PilotResponse(UUID id, String name, String description, String status,
                          int deviceCount, Instant createdAt, Instant startedAt, Instant completedAt) {}

    record PilotDetailResponse(UUID id, String name, String description, String status,
                                int deviceCount, Instant createdAt, Instant startedAt, Instant completedAt,
                                List<PilotDeviceResponse> devices) {}

    record PilotDeviceResponse(UUID id, String deviceId, String serialNumber, String siteName,
                                String circuitDescription, String status, Instant installedAt) {}

    record PilotObservationResponse(UUID id, String authorName, String type, String content,
                                     String deviceId, String siteName, Instant createdAt) {}

    record PilotIncidentResponse(UUID id, String severity, String category, String title,
                                  String description, String status, String deviceId,
                                  String siteName, Instant reportedAt) {}

    record PilotKpiResponse(UUID id, LocalDate date, int totalDevices, int activeDevices,
                             double uptimePercent, double telemetryValidPercent,
                             double avgSignalQuality, int alertsGenerated,
                             int alertsConfirmed, int falsePositives, double falsePositiveRate,
                             int incidentsOpen, int incidentsResolved, Instant computedAt) {}

    // --- Mappers ---

    private PilotResponse toResponse(PilotProgram p) {
        return new PilotResponse(p.getId(), p.getName(), p.getDescription(),
                p.getStatus().name(), p.getDeviceCount(),
                p.getCreatedAt(), p.getStartedAt(), p.getCompletedAt());
    }

    private PilotDetailResponse toDetailResponse(PilotProgram p) {
        List<PilotDeviceResponse> devices = p.getDevices().stream()
                .map(this::toDeviceResponse).toList();
        return new PilotDetailResponse(p.getId(), p.getName(), p.getDescription(),
                p.getStatus().name(), p.getDeviceCount(),
                p.getCreatedAt(), p.getStartedAt(), p.getCompletedAt(), devices);
    }

    private PilotDeviceResponse toDeviceResponse(PilotDevice d) {
        return new PilotDeviceResponse(d.id(), d.deviceId(), d.serialNumber(),
                d.siteName(), d.circuitDescription(), d.status().name(), d.installedAt());
    }

    private PilotObservationResponse toObservationResponse(PilotObservation o) {
        return new PilotObservationResponse(o.id(), o.authorName(), o.type().name(),
                o.content(), o.deviceId(), o.siteName(), o.createdAt());
    }

    private PilotIncidentResponse toIncidentResponse(PilotIncident i) {
        return new PilotIncidentResponse(i.id(), i.severity().name(), i.category().name(),
                i.title(), i.description(), i.status().name(), i.deviceId(),
                i.siteName(), i.reportedAt());
    }

    private PilotKpiResponse toKpiResponse(PilotKpiSnapshot k) {
        return new PilotKpiResponse(k.id(), k.date(), k.totalDevices(), k.activeDevices(),
                k.uptimePercent(), k.telemetryValidPercent(), k.avgSignalQuality(),
                k.alertsGenerated(), k.alertsConfirmed(), k.falsePositives(),
                k.falsePositiveRate(), k.incidentsOpen(), k.incidentsResolved(), k.computedAt());
    }
}
