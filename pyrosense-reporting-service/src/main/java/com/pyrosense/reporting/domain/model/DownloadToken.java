package com.pyrosense.reporting.domain.model;

import com.pyrosense.shared.util.ClockProvider;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public record DownloadToken(
        String token,
        UUID reportId,
        Instant expiresAt
) {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    public DownloadToken {
        Objects.requireNonNull(token);
        Objects.requireNonNull(reportId);
        Objects.requireNonNull(expiresAt);
    }

    public static DownloadToken create(UUID reportId) {
        return create(reportId, DEFAULT_TTL);
    }

    public static DownloadToken create(UUID reportId, Duration ttl) {
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        return new DownloadToken(token, reportId, ClockProvider.now().plus(ttl));
    }

    public boolean isExpired() {
        return ClockProvider.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isExpired();
    }
}
