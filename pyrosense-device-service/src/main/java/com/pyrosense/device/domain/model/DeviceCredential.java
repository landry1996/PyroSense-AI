package com.pyrosense.device.domain.model;

import com.pyrosense.shared.id.DeviceId;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class DeviceCredential {

    private final UUID id;
    private final DeviceId deviceId;
    private final String hmacKeyHash;
    private CredentialStatus status;
    private final Instant issuedAt;
    private Instant revokedAt;
    private final int version;

    private DeviceCredential(UUID id, DeviceId deviceId, String hmacKeyHash,
                             CredentialStatus status, Instant issuedAt, int version) {
        this.id = Objects.requireNonNull(id);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.hmacKeyHash = Objects.requireNonNull(hmacKeyHash);
        this.status = Objects.requireNonNull(status);
        this.issuedAt = Objects.requireNonNull(issuedAt);
        this.version = version;
    }

    public record CredentialPair(String plainHmacKey, DeviceCredential credential) {}

    public static CredentialPair issue(DeviceId deviceId, int version) {
        var random = new SecureRandom();
        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);
        String plainKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        String hash = hashKey(plainKey);

        var credential = new DeviceCredential(
                UUID.randomUUID(), deviceId, hash,
                CredentialStatus.ACTIVE, Instant.now(), version);
        return new CredentialPair(plainKey, credential);
    }

    public static DeviceCredential reconstitute(UUID id, DeviceId deviceId, String hmacKeyHash,
                                                 CredentialStatus status, Instant issuedAt,
                                                 Instant revokedAt, int version) {
        var cred = new DeviceCredential(id, deviceId, hmacKeyHash, status, issuedAt, version);
        cred.revokedAt = revokedAt;
        return cred;
    }

    public void revoke() {
        if (this.status == CredentialStatus.REVOKED) return;
        this.status = CredentialStatus.REVOKED;
        this.revokedAt = Instant.now();
    }

    public boolean isActive() {
        return status == CredentialStatus.ACTIVE;
    }

    private static String hashKey(String plainKey) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public String getHmacKeyHash() { return hmacKeyHash; }
    public CredentialStatus getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public int getVersion() { return version; }
}
