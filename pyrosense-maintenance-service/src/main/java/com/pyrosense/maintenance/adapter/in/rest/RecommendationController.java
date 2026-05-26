package com.pyrosense.maintenance.adapter.in.rest;

import com.pyrosense.maintenance.application.port.in.ManageRecommendationUseCase;
import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.maintenance.domain.model.InterventionRecommendation;
import com.pyrosense.shared.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final ManageRecommendationUseCase manageRecommendationUseCase;

    public RecommendationController(ManageRecommendationUseCase manageRecommendationUseCase) {
        this.manageRecommendationUseCase = manageRecommendationUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<List<RecommendationResponse>> listPending() {
        var tenantId = TenantContext.require();
        List<InterventionRecommendation> pending = manageRecommendationUseCase.findPendingByTenant(tenantId);
        return ResponseEntity.ok(pending.stream().map(this::toResponse).toList());
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<AcceptResponse> accept(@PathVariable UUID id) {
        Intervention intervention = manageRecommendationUseCase.acceptRecommendation(id);
        return ResponseEntity.ok(new AcceptResponse(
                id.toString(),
                intervention.getId().toString(),
                intervention.getStatus().name(),
                intervention.getPriority().name()
        ));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<RecommendationResponse> reject(@PathVariable UUID id,
                                                          @Valid @RequestBody RejectRequest request) {
        InterventionRecommendation rejected = manageRecommendationUseCase.rejectRecommendation(id, request.reason());
        return ResponseEntity.ok(toResponse(rejected));
    }

    private RecommendationResponse toResponse(InterventionRecommendation r) {
        return new RecommendationResponse(
                r.getId().toString(),
                r.getTenantId().value().toString(),
                r.getAlertId().value().toString(),
                r.getDeviceId().value().toString(),
                r.getSuggestedType().name(),
                r.getSuggestedPriority().name(),
                r.getReason(),
                r.getStatus().name(),
                r.getSlaExpiresAt(),
                r.isSlaBreached(),
                r.getAcceptedInterventionId() != null ? r.getAcceptedInterventionId().toString() : null,
                r.getRejectionReason(),
                r.getCreatedAt(),
                r.getDecidedAt()
        );
    }

    record RejectRequest(@NotBlank String reason) {}

    record RecommendationResponse(
            String id, String tenantId, String alertId, String deviceId,
            String suggestedType, String suggestedPriority, String reason, String status,
            Instant slaExpiresAt, boolean slaBreached, String acceptedInterventionId,
            String rejectionReason, Instant createdAt, Instant decidedAt
    ) {}

    record AcceptResponse(String recommendationId, String interventionId,
                           String interventionStatus, String priority) {}
}
