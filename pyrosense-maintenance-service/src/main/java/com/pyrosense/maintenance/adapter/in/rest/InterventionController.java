package com.pyrosense.maintenance.adapter.in.rest;

import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase;
import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase.CreateInterventionCommand;
import com.pyrosense.maintenance.application.port.in.GetInterventionQuery;
import com.pyrosense.maintenance.application.port.in.GetInterventionQuery.InterventionStatistics;
import com.pyrosense.maintenance.application.port.in.ManageInterventionUseCase;
import com.pyrosense.maintenance.domain.model.*;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.security.TenantContext;
import com.pyrosense.shared.util.ClockProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interventions")
public class InterventionController {

    private final CreateInterventionUseCase createUseCase;
    private final ManageInterventionUseCase manageUseCase;
    private final GetInterventionQuery queryUseCase;

    public InterventionController(CreateInterventionUseCase createUseCase,
                                  ManageInterventionUseCase manageUseCase,
                                  GetInterventionQuery queryUseCase) {
        this.createUseCase = createUseCase;
        this.manageUseCase = manageUseCase;
        this.queryUseCase = queryUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<InterventionResponse> create(@Valid @RequestBody CreateInterventionRequest request) {
        var command = new CreateInterventionCommand(
                new TenantId(UUID.fromString(request.tenantId())),
                AlertId.from(request.alertId()),
                new DeviceId(UUID.fromString(request.deviceId())),
                request.severity(),
                request.alertType(),
                request.description()
        );
        Intervention result = createUseCase.createFromAlert(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<InterventionResponse> schedule(@PathVariable UUID id,
                                                         @Valid @RequestBody ScheduleRequest request) {
        Intervention result = manageUseCase.schedule(id, request.scheduledAt());
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<InterventionResponse> assign(@PathVariable UUID id,
                                                       @Valid @RequestBody AssignRequest request) {
        Intervention result = manageUseCase.assign(id,
                new UserId(UUID.fromString(request.electricianId())), request.scheduledAt());
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN')")
    public ResponseEntity<InterventionResponse> start(@PathVariable UUID id) {
        Intervention result = manageUseCase.start(id);
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{id}/diagnostic")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN')")
    public ResponseEntity<InterventionResponse> addDiagnostic(@PathVariable UUID id,
                                                              @Valid @RequestBody DiagnosticRequest request) {
        var diagnostic = new FieldDiagnostic(
                request.observations(), request.measurementsTaken(),
                request.recommendations(), request.diagnosticBy(), ClockProvider.now());
        Intervention result = manageUseCase.addDiagnostic(id, diagnostic);
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN')")
    public ResponseEntity<InterventionResponse> complete(@PathVariable UUID id,
                                                         @Valid @RequestBody CompleteRequest request) {
        InterventionResult resultEnum = InterventionResult.valueOf(request.result());
        Intervention result = manageUseCase.complete(id, resultEnum);
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{id}/risk-impact")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<InterventionResponse> recordRiskImpact(@PathVariable UUID id,
                                                                  @Valid @RequestBody RiskImpactRequest request) {
        var impact = new RiskImpact(request.riskScoreBefore(), request.riskScoreAfter(),
                request.avoidedIncidentEstimateDays());
        Intervention result = manageUseCase.recordRiskImpact(id, impact);
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<InterventionResponse> cancel(@PathVariable UUID id) {
        Intervention result = manageUseCase.cancel(id);
        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'SUPPORT_READONLY')")
    public ResponseEntity<InterventionResponse> getById(@PathVariable UUID id) {
        TenantId currentTenant = TenantContext.require();
        return queryUseCase.findById(id)
                .filter(intervention -> intervention.getTenantId().equals(currentTenant))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'SUPPORT_READONLY')")
    public ResponseEntity<List<InterventionResponse>> list(@RequestParam(required = false) String status,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(size, 200);
        int offset = page * safeSize;
        TenantId tid = TenantContext.require();
        List<Intervention> interventions = (status != null)
                ? queryUseCase.findByTenantAndStatus(tid, InterventionStatus.valueOf(status))
                : queryUseCase.findByTenant(tid);
        // TODO: Replace in-memory pagination with proper SQL LIMIT/OFFSET
        return ResponseEntity.ok(interventions.stream()
                .skip(offset).limit(safeSize)
                .map(this::toResponse).toList());
    }

    @GetMapping("/electrician/{electricianId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'SUPPORT_READONLY')")
    public ResponseEntity<List<InterventionResponse>> listByElectrician(
            @PathVariable String electricianId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(size, 200);
        int offset = page * safeSize;
        TenantId currentTenant = TenantContext.require();
        List<Intervention> interventions = queryUseCase.findByElectrician(
                new UserId(UUID.fromString(electricianId)));
        // TODO: Replace in-memory pagination with proper SQL LIMIT/OFFSET
        return ResponseEntity.ok(interventions.stream()
                .filter(i -> i.getTenantId().equals(currentTenant))
                .skip(offset).limit(safeSize)
                .map(this::toResponse).toList());
    }

    @GetMapping("/device/{deviceId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'SUPPORT_READONLY')")
    public ResponseEntity<List<InterventionResponse>> listByDevice(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(size, 200);
        int offset = page * safeSize;
        TenantId currentTenant = TenantContext.require();
        List<Intervention> interventions = queryUseCase.findByDevice(new DeviceId(UUID.fromString(deviceId)));
        // TODO: Replace in-memory pagination with proper SQL LIMIT/OFFSET
        return ResponseEntity.ok(interventions.stream()
                .filter(i -> i.getTenantId().equals(currentTenant))
                .skip(offset).limit(safeSize)
                .map(this::toResponse).toList());
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'SUPPORT_READONLY')")
    public ResponseEntity<InterventionStatistics> statistics() {
        TenantId currentTenant = TenantContext.require();
        return ResponseEntity.ok(queryUseCase.getStatistics(currentTenant));
    }

    private InterventionResponse toResponse(Intervention i) {
        return new InterventionResponse(
                i.getId().toString(),
                i.getTenantId().value().toString(),
                i.getSourceAlertId().value().toString(),
                i.getDeviceId().value().toString(),
                i.getType().name(),
                i.getPriority().name(),
                i.getStatus().name(),
                i.getDescription(),
                i.getAssignedElectricianId() != null ? i.getAssignedElectricianId().value().toString() : null,
                i.getScheduledAt(),
                i.getStartedAt(),
                i.getCompletedAt(),
                i.getResult() != null ? i.getResult().name() : null,
                i.getRiskImpact() != null ? new RiskImpactResponse(
                        i.getRiskImpact().riskScoreBefore(),
                        i.getRiskImpact().riskScoreAfter(),
                        i.getRiskImpact().avoidedIncidentEstimateDays(),
                        i.getRiskImpact().riskReduction()
                ) : null,
                i.getDiagnostic() != null ? new DiagnosticResponse(
                        i.getDiagnostic().observations(),
                        i.getDiagnostic().measurementsTaken(),
                        i.getDiagnostic().recommendations(),
                        i.getDiagnostic().diagnosticBy(),
                        i.getDiagnostic().recordedAt()
                ) : null,
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }

    // Request DTOs
    record CreateInterventionRequest(
            @NotBlank String tenantId,
            @NotBlank String alertId,
            @NotBlank String deviceId,
            @NotBlank String severity,
            String alertType,
            @NotBlank String description
    ) {}

    record ScheduleRequest(@NotNull Instant scheduledAt) {}

    record AssignRequest(@NotBlank String electricianId, @NotNull Instant scheduledAt) {}

    record DiagnosticRequest(
            @NotBlank String observations,
            String measurementsTaken,
            String recommendations,
            @NotBlank String diagnosticBy
    ) {}

    record CompleteRequest(@NotBlank String result) {}

    record RiskImpactRequest(int riskScoreBefore, int riskScoreAfter, Integer avoidedIncidentEstimateDays) {}

    // Response DTOs
    record InterventionResponse(
            String id, String tenantId, String alertId, String deviceId,
            String type, String priority, String status, String description,
            String assignedElectricianId, Instant scheduledAt, Instant startedAt,
            Instant completedAt, String result, RiskImpactResponse riskImpact,
            DiagnosticResponse diagnostic, Instant createdAt, Instant updatedAt
    ) {}

    record RiskImpactResponse(int riskScoreBefore, int riskScoreAfter,
                               Integer avoidedIncidentEstimateDays, int riskReduction) {}

    record DiagnosticResponse(String observations, String measurementsTaken,
                               String recommendations, String diagnosticBy, Instant recordedAt) {}
}
