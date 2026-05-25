package com.pyrosense.device.adapter.in.rest;

import com.pyrosense.device.adapter.in.rest.dto.BuildingResponse;
import com.pyrosense.device.application.port.in.GetBuildingQuery;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buildings")
@Tag(name = "Buildings", description = "Building-level aggregated data")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
public class BuildingController {

    private final GetBuildingQuery getBuildingQuery;

    public BuildingController(GetBuildingQuery getBuildingQuery) {
        this.getBuildingQuery = getBuildingQuery;
    }

    @GetMapping
    @Operation(summary = "List buildings for current tenant with aggregated device stats")
    public ResponseEntity<List<BuildingResponse>> listBuildings() {
        TenantId tenantId = TenantContext.require();
        var summaries = getBuildingQuery.listByTenant(tenantId);
        var response = summaries.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get building detail by ID")
    public ResponseEntity<BuildingResponse> getById(@PathVariable String id) {
        TenantId tenantId = TenantContext.require();
        var summary = getBuildingQuery.getById(BuildingId.from(id), tenantId);
        return ResponseEntity.ok(toResponse(summary));
    }

    private BuildingResponse toResponse(GetBuildingQuery.BuildingSummary summary) {
        return new BuildingResponse(
                summary.buildingId().toString(),
                summary.name(),
                summary.address(),
                summary.totalDevices(),
                summary.activeDevices(),
                summary.riskScore(),
                summary.status(),
                summary.lastAlertAt()
        );
    }
}
