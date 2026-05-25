package com.pyrosense.identity.adapter.in.rest;

import com.pyrosense.identity.application.port.in.ManageDeviceCredentialUseCase;
import com.pyrosense.identity.application.port.in.ManageDeviceCredentialUseCase.DeviceCredentialResult;
import com.pyrosense.identity.config.Audited;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/device")
public class DeviceAuthController {

    private final ManageDeviceCredentialUseCase credentialUseCase;

    public DeviceAuthController(ManageDeviceCredentialUseCase credentialUseCase) {
        this.credentialUseCase = credentialUseCase;
    }

    @PostMapping("/credentials")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    @Audited(value = "DEVICE_CREDENTIAL_ISSUED", resourceType = "DeviceCredential")
    public ResponseEntity<CredentialResponse> issueCredential(@Valid @RequestBody IssueCredentialRequest request) {
        DeviceCredentialResult result = credentialUseCase.issueCredential(
                DeviceId.from(request.deviceId()),
                new TenantId(UUID.fromString(request.tenantId())),
                Duration.ofDays(request.validityDays()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CredentialResponse(result.deviceId().value().toString(), result.token(), result.expiresAt()));
    }

    @PostMapping("/credentials/{deviceId}/rotate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Audited(value = "DEVICE_CREDENTIAL_ROTATED", resourceType = "DeviceCredential")
    public ResponseEntity<CredentialResponse> rotateCredential(@PathVariable String deviceId) {
        DeviceCredentialResult result = credentialUseCase.rotateCredential(DeviceId.from(deviceId));
        return ResponseEntity.ok(new CredentialResponse(result.deviceId().value().toString(), result.token(), result.expiresAt()));
    }

    @DeleteMapping("/credentials/{deviceId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Audited(value = "DEVICE_CREDENTIAL_REVOKED", resourceType = "DeviceCredential")
    public ResponseEntity<Void> revokeCredential(@PathVariable String deviceId) {
        credentialUseCase.revokeCredential(DeviceId.from(deviceId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        boolean valid = credentialUseCase.validateToken(DeviceId.from(request.deviceId()), request.token());
        return ResponseEntity.ok(new ValidationResponse(valid));
    }

    record IssueCredentialRequest(@NotBlank String deviceId, @NotBlank String tenantId, int validityDays) {
        IssueCredentialRequest { if (validityDays <= 0) validityDays = 90; }
    }
    record ValidateTokenRequest(@NotBlank String deviceId, @NotBlank String token) {}
    record CredentialResponse(String deviceId, String token, Instant expiresAt) {}
    record ValidationResponse(boolean valid) {}
}
