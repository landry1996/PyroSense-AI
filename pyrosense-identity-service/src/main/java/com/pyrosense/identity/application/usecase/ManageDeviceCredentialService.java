package com.pyrosense.identity.application.usecase;

import com.pyrosense.identity.application.port.in.ManageDeviceCredentialUseCase;
import com.pyrosense.identity.application.port.out.DeviceCredentialRepository;
import com.pyrosense.identity.domain.model.DeviceCredential;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class ManageDeviceCredentialService implements ManageDeviceCredentialUseCase {

    private final DeviceCredentialRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ManageDeviceCredentialService(DeviceCredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    public DeviceCredentialResult issueCredential(DeviceId deviceId, TenantId tenantId, Duration validity) {
        repository.findActiveByDeviceId(deviceId).ifPresent(existing -> existing.revoke());

        String token = generateToken();
        String tokenHash = hashToken(token);
        Instant expiresAt = ClockProvider.now().plus(validity);

        DeviceCredential credential = new DeviceCredential(
                UUID.randomUUID(), deviceId, tenantId, tokenHash, expiresAt);
        repository.save(credential);

        return new DeviceCredentialResult(deviceId, token, expiresAt);
    }

    @Override
    public DeviceCredentialResult rotateCredential(DeviceId deviceId) {
        DeviceCredential credential = repository.findActiveByDeviceId(deviceId)
                .orElseThrow(() -> new NotFoundException("DeviceCredential", deviceId.value()));

        String newToken = generateToken();
        String newHash = hashToken(newToken);
        Instant newExpiry = ClockProvider.now().plus(Duration.ofDays(90));

        credential.rotate(newHash, newExpiry);
        repository.save(credential);

        return new DeviceCredentialResult(deviceId, newToken, newExpiry);
    }

    @Override
    public void revokeCredential(DeviceId deviceId) {
        repository.findActiveByDeviceId(deviceId).ifPresent(credential -> {
            credential.revoke();
            repository.save(credential);
        });
    }

    @Override
    public boolean validateToken(DeviceId deviceId, String token) {
        return repository.findActiveByDeviceId(deviceId)
                .filter(DeviceCredential::isValid)
                .map(credential -> {
                    String hash = hashToken(token);
                    if (credential.getTokenHash().equals(hash)) {
                        credential.recordAuthentication();
                        repository.save(credential);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
