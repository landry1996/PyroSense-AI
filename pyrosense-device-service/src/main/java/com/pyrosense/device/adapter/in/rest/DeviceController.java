package com.pyrosense.device.adapter.in.rest;

import com.pyrosense.device.adapter.in.rest.dto.*;
import com.pyrosense.device.application.port.in.*;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.pagination.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Device lifecycle management")
public class DeviceController {

    private final RegisterDeviceUseCase registerDevice;
    private final ProvisionDeviceUseCase provisionDevice;
    private final ActivateDeviceUseCase activateDevice;
    private final RevokeDeviceUseCase revokeDevice;
    private final GetDeviceQuery getDevice;
    private final RecordHeartbeatUseCase recordHeartbeat;

    public DeviceController(RegisterDeviceUseCase registerDevice,
                            ProvisionDeviceUseCase provisionDevice,
                            ActivateDeviceUseCase activateDevice,
                            RevokeDeviceUseCase revokeDevice,
                            GetDeviceQuery getDevice,
                            RecordHeartbeatUseCase recordHeartbeat) {
        this.registerDevice = registerDevice;
        this.provisionDevice = provisionDevice;
        this.activateDevice = activateDevice;
        this.revokeDevice = revokeDevice;
        this.getDevice = getDevice;
        this.recordHeartbeat = recordHeartbeat;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVICE_MANAGER')")
    @Operation(summary = "Register a new device")
    public ResponseEntity<RegisterDeviceResponse> register(
            @Valid @RequestBody RegisterDeviceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var command = new RegisterDeviceUseCase.RegisterDeviceCommand(
                request.serialNumber(),
                request.firmwareVersion(),
                request.hardwareRevision(),
                request.connectivityType()
        );
        var result = registerDevice.execute(command, extractActor(jwt));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterDeviceResponse(result.deviceId().value(), result.enrollmentKey()));
    }

    @PostMapping("/{deviceId}/provision")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVICE_MANAGER')")
    @Operation(summary = "Provision a device to a tenant/building/panel")
    public ResponseEntity<Void> provision(
            @PathVariable UUID deviceId,
            @Valid @RequestBody ProvisionDeviceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var command = new ProvisionDeviceUseCase.ProvisionDeviceCommand(
                DeviceId.from(deviceId.toString()),
                TenantId.from(request.tenantId().toString()),
                BuildingId.from(request.buildingId().toString()),
                ElectricalPanelId.from(request.panelId().toString())
        );
        provisionDevice.execute(command, extractActor(jwt));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVICE_MANAGER')")
    @Operation(summary = "Activate a provisioned device")
    public ResponseEntity<Void> activate(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal Jwt jwt) {
        activateDevice.execute(DeviceId.from(deviceId.toString()), extractActor(jwt));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVICE_MANAGER')")
    @Operation(summary = "Revoke a device")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID deviceId,
            @Valid @RequestBody RevokeDeviceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var command = new RevokeDeviceUseCase.RevokeDeviceCommand(
                DeviceId.from(deviceId.toString()),
                request.reason()
        );
        revokeDevice.execute(command, extractActor(jwt));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/heartbeat")
    @Operation(summary = "Record a device heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable UUID deviceId) {
        recordHeartbeat.execute(DeviceId.from(deviceId.toString()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get device by ID")
    public ResponseEntity<DeviceResponse> getById(@PathVariable UUID deviceId) {
        var device = getDevice.getById(DeviceId.from(deviceId.toString()));
        return ResponseEntity.ok(DeviceResponseMapper.toResponse(device));
    }

    @GetMapping("/serial/{serialNumber}")
    @Operation(summary = "Get device by serial number")
    public ResponseEntity<DeviceResponse> getBySerialNumber(@PathVariable String serialNumber) {
        var device = getDevice.getBySerialNumber(serialNumber);
        return ResponseEntity.ok(DeviceResponseMapper.toResponse(device));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "List devices for a tenant")
    public ResponseEntity<DevicePageResponse> listByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageRequest = PageRequest.of(page, size);
        var result = getDevice.listByTenant(TenantId.from(tenantId.toString()), pageRequest);
        return ResponseEntity.ok(DeviceResponseMapper.toPageResponse(result));
    }

    private String extractActor(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username");
    }
}
