package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class DeviceCredential {

    private final UUID id;
    private final DeviceId deviceId;
    private final TenantId tenantId;
    private String tokenHash;
    private Instant expiresAt;
    private boolean active;
    private final Instant createdAt;
    private Instant lastAuthenticatedAt;
    private int authenticationCount;

    public DeviceCredential(UUID id, DeviceId deviceId, TenantId tenantId, String tokenHash, Instant expiresAt) {
        this.id = Objects.requireNonNull(id);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.active = true;
        this.createdAt = ClockProvider.now();
        this.authenticationCount = 0;
    }

    private DeviceCredential(UUID id, DeviceId deviceId, TenantId tenantId, String tokenHash,
                             Instant expiresAt, boolean active, Instant createdAt,
                             Instant lastAuthenticatedAt, int authenticationCount) {
        this.id = id;
        this.deviceId = deviceId;
        this.tenantId = tenantId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.active = active;
        this.createdAt = createdAt;
        this.lastAuthenticatedAt = lastAuthenticatedAt;
        this.authenticationCount = authenticationCount;
    }

    public static DeviceCredential reconstitute(UUID id, DeviceId deviceId, TenantId tenantId,
                                                 String tokenHash, Instant expiresAt, boolean active,
                                                 Instant createdAt, Instant lastAuthenticatedAt,
                                                 int authenticationCount) {
        return new DeviceCredential(id, deviceId, tenantId, tokenHash, expiresAt, active,
                createdAt, lastAuthenticatedAt, authenticationCount);
    }

    public boolean isValid() {
        return active && ClockProvider.now().isBefore(expiresAt);
    }

    public void recordAuthentication() {
        this.lastAuthenticatedAt = ClockProvider.now();
        this.authenticationCount++;
    }

    public void rotate(String newTokenHash, Instant newExpiresAt) {
        this.tokenHash = Objects.requireNonNull(newTokenHash);
        this.expiresAt = Objects.requireNonNull(newExpiresAt);
    }

    public void revoke() {
        this.active = false;
    }

    public boolean isExpiringSoon(java.time.Duration threshold) {
        return active && ClockProvider.now().plus(threshold).isAfter(expiresAt);
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public TenantId getTenantId() { return tenantId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAuthenticatedAt() { return lastAuthenticatedAt; }
    public int getAuthenticationCount() { return authenticationCount; }
}
