package com.pyrosense.device.adapter.in.rest;

import com.pyrosense.device.adapter.in.rest.dto.*;
import com.pyrosense.device.application.port.in.*;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@Tag(name = "Device Provisioning", description = "Secure device enrollment and credential management")
public class ProvisioningController {

    private final CreateClaimTokenUseCase createClaimToken;
    private final DeviceProvisioningUseCase deviceProvisioning;
    private final RotateDeviceCredentialUseCase rotateCredential;

    public ProvisioningController(CreateClaimTokenUseCase createClaimToken,
                                   DeviceProvisioningUseCase deviceProvisioning,
                                   RotateDeviceCredentialUseCase rotateCredential) {
        this.createClaimToken = createClaimToken;
        this.deviceProvisioning = deviceProvisioning;
        this.rotateCredential = rotateCredential;
    }

    @PostMapping("/{deviceId}/claim-token")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVICE_MANAGER', 'PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Generate a one-time claim token for device provisioning")
    public ResponseEntity<ClaimTokenResponse> createClaimToken(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal Jwt jwt) {
        TenantId tenantId = TenantContext.require();
        var command = new CreateClaimTokenUseCase.CreateClaimTokenCommand(
                DeviceId.from(deviceId.toString()), tenantId);
        var result = createClaimToken.execute(command, extractActor(jwt));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ClaimTokenResponse(result.token(), result.expiresAt()));
    }

    @PostMapping("/provision")
    @Operation(summary = "Provision a device using a claim token (called by device)")
    public ResponseEntity<ProvisioningResponse> provision(
            @Valid @RequestBody ProvisionDeviceWithTokenRequest request,
            HttpServletRequest httpRequest) {
        String sourceIp = getClientIp(httpRequest);
        var command = new DeviceProvisioningUseCase.ProvisionWithTokenCommand(
                request.claimToken(),
                request.deviceSerial(),
                request.deviceModel(),
                request.firmwareVersion(),
                sourceIp);
        var result = deviceProvisioning.execute(command);
        return ResponseEntity.ok(new ProvisioningResponse(
                result.deviceId(), result.tenantId(),
                result.hmacKey(), result.mqttBrokerUri(),
                result.mqttPort(), result.topicPrefix()));
    }

    @PostMapping("/{deviceId}/credentials/rotate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVICE_MANAGER', 'PLATFORM_ADMIN')")
    @Operation(summary = "Rotate device HMAC credentials")
    public ResponseEntity<CredentialRotationResponse> rotateCredentials(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal Jwt jwt) {
        var result = rotateCredential.execute(DeviceId.from(deviceId.toString()), extractActor(jwt));
        return ResponseEntity.ok(new CredentialRotationResponse(result.newHmacKey(), result.version()));
    }


    private String extractActor(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username");
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
