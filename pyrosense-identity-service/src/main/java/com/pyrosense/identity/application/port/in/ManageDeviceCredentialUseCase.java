package com.pyrosense.identity.application.port.in;

import com.pyrosense.identity.domain.model.DeviceCredential;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Duration;

public interface ManageDeviceCredentialUseCase {

    DeviceCredentialResult issueCredential(DeviceId deviceId, TenantId tenantId, Duration validity);

    DeviceCredentialResult rotateCredential(DeviceId deviceId);

    void revokeCredential(DeviceId deviceId);

    boolean validateToken(DeviceId deviceId, String token);

    record DeviceCredentialResult(DeviceId deviceId, String token, java.time.Instant expiresAt) {}
}
