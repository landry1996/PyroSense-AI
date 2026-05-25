package com.pyrosense.reporting.domain;

import com.pyrosense.reporting.domain.model.DownloadToken;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DownloadTokenTest {

    @Test
    void shouldCreateTokenWithDefaultTtl() {
        UUID reportId = UUID.randomUUID();
        DownloadToken token = DownloadToken.create(reportId);

        assertNotNull(token.token());
        assertEquals(reportId, token.reportId());
        assertNotNull(token.expiresAt());
        assertTrue(token.isValid());
        assertFalse(token.isExpired());
    }

    @Test
    void shouldCreateTokenWithCustomTtl() {
        UUID reportId = UUID.randomUUID();
        DownloadToken token = DownloadToken.create(reportId, Duration.ofHours(1));

        assertTrue(token.isValid());
    }

    @Test
    void shouldGenerateUniqueTokens() {
        UUID reportId = UUID.randomUUID();
        DownloadToken t1 = DownloadToken.create(reportId);
        DownloadToken t2 = DownloadToken.create(reportId);

        assertNotEquals(t1.token(), t2.token());
    }

    @Test
    void shouldHaveBase64UrlEncodedToken() {
        DownloadToken token = DownloadToken.create(UUID.randomUUID());
        assertTrue(token.token().length() >= 32);
        assertFalse(token.token().contains("+"));
        assertFalse(token.token().contains("/"));
        assertFalse(token.token().contains("="));
    }

    @Test
    void shouldRejectNullToken() {
        assertThrows(NullPointerException.class,
                () -> new DownloadToken(null, UUID.randomUUID(), java.time.Instant.now()));
    }

    @Test
    void shouldRejectNullReportId() {
        assertThrows(NullPointerException.class,
                () -> new DownloadToken("token", null, java.time.Instant.now()));
    }

    @Test
    void shouldRejectNullExpiresAt() {
        assertThrows(NullPointerException.class,
                () -> new DownloadToken("token", UUID.randomUUID(), null));
    }
}
