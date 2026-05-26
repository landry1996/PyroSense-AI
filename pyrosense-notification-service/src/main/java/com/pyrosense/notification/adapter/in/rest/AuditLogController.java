package com.pyrosense.notification.adapter.in.rest;

import com.pyrosense.notification.application.port.in.GetAuditLogQuery;
import com.pyrosense.notification.application.port.in.GetAuditLogQuery.AuditLogFilter;
import com.pyrosense.notification.application.port.in.GetAuditLogQuery.AuditLogPage;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.AuditEntry;
import com.pyrosense.shared.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
public class AuditLogController {

    private final GetAuditLogQuery auditLogQuery;

    public AuditLogController(GetAuditLogQuery auditLogQuery) {
        this.auditLogQuery = auditLogQuery;
    }

    @GetMapping
    public ResponseEntity<AuditLogPageResponse> list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        TenantId tenantId = TenantContext.require();
        Instant fromInstant = from != null ? Instant.parse(from) : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant toInstant = to != null ? Instant.parse(to) : Instant.now();

        AuditLogFilter filter = new AuditLogFilter(fromInstant, toInstant, action, resourceType, page, size);
        AuditLogPage result = auditLogQuery.findByTenant(tenantId, filter);

        List<AuditLogResponse> entries = result.entries().stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new AuditLogPageResponse(entries, result.totalCount(), result.page(),
                result.size(), result.totalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponse> getById(@PathVariable String id) {
        TenantId tenantId = TenantContext.require();
        return auditLogQuery.findById(UUID.fromString(id), tenantId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private AuditLogResponse toResponse(AuditEntry entry) {
        return new AuditLogResponse(
                entry.id().toString(),
                entry.action(),
                entry.resourceType(),
                entry.resourceId(),
                entry.userId() != null ? entry.userId().value().toString() : null,
                entry.actorRole(),
                entry.tenantId() != null ? entry.tenantId().value().toString() : null,
                entry.ipAddress(),
                entry.correlationId(),
                entry.details(),
                entry.metadata(),
                entry.timestamp()
        );
    }

    record AuditLogResponse(String id, String action, String resourceType, String resourceId,
                             String actorId, String actorRole, String tenantId,
                             String ipAddress, String correlationId, String details,
                             Map<String, String> metadata, Instant timestamp) {}

    record AuditLogPageResponse(List<AuditLogResponse> entries, long totalCount, int page, int size, int totalPages) {}
}
