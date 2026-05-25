package com.pyrosense.identity.adapter.in.rest;

import com.pyrosense.identity.application.port.in.ManageTenantUseCase;
import com.pyrosense.identity.application.port.in.ManageTenantUseCase.CreateTenantCommand;
import com.pyrosense.identity.config.Audited;
import com.pyrosense.identity.domain.model.Tenant;
import com.pyrosense.shared.id.TenantId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final ManageTenantUseCase manageTenantUseCase;

    public TenantController(ManageTenantUseCase manageTenantUseCase) {
        this.manageTenantUseCase = manageTenantUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Audited(value = "TENANT_CREATED", resourceType = "Tenant")
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = manageTenantUseCase.create(new CreateTenantCommand(request.name(), request.slug()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tenant));
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<TenantResponse> getById(@PathVariable String tenantId) {
        return manageTenantUseCase.findById(new TenantId(UUID.fromString(tenantId)))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{tenantId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Audited(value = "TENANT_DEACTIVATED", resourceType = "Tenant")
    public ResponseEntity<Void> deactivate(@PathVariable String tenantId) {
        manageTenantUseCase.deactivate(new TenantId(UUID.fromString(tenantId)));
        return ResponseEntity.noContent().build();
    }

    private TenantResponse toResponse(Tenant t) {
        return new TenantResponse(t.getId().value().toString(), t.getName(), t.getSlug(), t.isActive(), t.getCreatedAt());
    }

    record CreateTenantRequest(@NotBlank String name,
                                @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") String slug) {}
    record TenantResponse(String id, String name, String slug, boolean active, Instant createdAt) {}
}
