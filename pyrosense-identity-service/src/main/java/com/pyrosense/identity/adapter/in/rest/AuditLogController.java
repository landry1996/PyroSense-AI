package com.pyrosense.identity.adapter.in.rest;

import com.pyrosense.identity.application.port.out.AuditLogRepository;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.AuditEntry;
import com.pyrosense.shared.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-log")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        TenantId tenantId = TenantContext.require();
        Instant fromInstant = from != null ? Instant.parse(from) : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant toInstant = to != null ? Instant.parse(to) : Instant.now();
        int safeSize = Math.min(size, 200);
        int offset = page * safeSize;

        List<AuditLogResponse> entries = auditLogRepository.findByTenant(tenantId, fromInstant, toInstant)
                .stream()
                .skip(offset).limit(safeSize)
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(entries);
    }

    private AuditLogResponse toResponse(AuditEntry entry) {
        return new AuditLogResponse(
                entry.id().toString(),
                entry.action(),
                entry.resourceType(),
                entry.resourceId(),
                entry.userId() != null ? entry.userId().value().toString() : null,
                entry.ipAddress(),
                entry.details(),
                entry.timestamp()
        );
    }

    record AuditLogResponse(String id, String action, String resourceType, String resourceId,
                             String userId, String ipAddress, String details, Instant timestamp) {}
}
