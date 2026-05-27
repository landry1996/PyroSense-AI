package com.pyrosense.dashboard.domain.model;

import java.time.Instant;

public record DeviceSecurityStatus(
        String deviceId,
        String credentialStatus,
        int credentialVersion,
        Instant lastRotation,
        int failedAuthAttempts,
        int replayAttemptsBlocked,
        boolean isRevoked,
        Instant lastAuthFailure
) {}
