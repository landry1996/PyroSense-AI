package com.pyrosense.device.domain.model;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class ClaimToken {

    private final UUID id;
    private final DeviceId deviceId;
    private final TenantId tenantId;
    private final String tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final String createdBy;
    private boolean consumed;
    private Instant consumedAt;

    private ClaimToken(UUID id, DeviceId deviceId, TenantId tenantId,
                       String tokenHash, Instant createdAt, Instant expiresAt, String createdBy) {
        this.id = Objects.requireNonNull(id);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.consumed = false;
    }

    public record TokenPair(String plainToken, ClaimToken claimToken) {}

    public static TokenPair create(DeviceId deviceId, TenantId tenantId,
                                    Duration validity, String actor) {
        var random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String hash = hashToken(plainToken);

        Instant now = Instant.now();
        var token = new ClaimToken(UUID.randomUUID(), deviceId, tenantId,
                hash, now, now.plus(validity), actor);
        return new TokenPair(plainToken, token);
    }

    public static ClaimToken reconstitute(UUID id, DeviceId deviceId, TenantId tenantId,
                                           String tokenHash, Instant createdAt, Instant expiresAt,
                                           String createdBy, boolean consumed, Instant consumedAt) {
        var token = new ClaimToken(id, deviceId, tenantId, tokenHash, createdAt, expiresAt, createdBy);
        token.consumed = consumed;
        token.consumedAt = consumedAt;
        return token;
    }

    public void consume() {
        if (consumed) {
            throw new IllegalStateException("Claim token already consumed");
        }
        if (isExpired()) {
            throw new IllegalStateException("Claim token expired");
        }
        this.consumed = true;
        this.consumedAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !consumed && !isExpired();
    }

    public boolean matches(String plainToken) {
        return hashToken(plainToken).equals(this.tokenHash);
    }

    private static String hashToken(String plainToken) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public TenantId getTenantId() { return tenantId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getCreatedBy() { return createdBy; }
    public boolean isConsumed() { return consumed; }
    public Instant getConsumedAt() { return consumedAt; }
}
