package com.pyrosense.device.application.port.in;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;

public interface CreateClaimTokenUseCase {

    record CreateClaimTokenCommand(DeviceId deviceId, TenantId tenantId) {}

    record ClaimTokenResult(String token, Instant expiresAt) {}

    ClaimTokenResult execute(CreateClaimTokenCommand command, String actor);
}
