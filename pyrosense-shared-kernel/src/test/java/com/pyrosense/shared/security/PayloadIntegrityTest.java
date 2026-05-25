package com.pyrosense.shared.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class PayloadIntegrityTest {

    private static final String SECRET = "test-device-secret-key-32bytes!!";
    private static final String BODY = "{\"temperature\":25.3,\"current\":1.2}";

    @Test
    void shouldSignAndVerify() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = PayloadIntegrity.sign(SECRET, timestamp, BODY);

        assertThat(PayloadIntegrity.verify(SECRET, timestamp, BODY, signature)).isTrue();
    }

    @Test
    void shouldRejectTamperedBody() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = PayloadIntegrity.sign(SECRET, timestamp, BODY);

        String tamperedBody = "{\"temperature\":99.9,\"current\":1.2}";
        assertThat(PayloadIntegrity.verify(SECRET, timestamp, tamperedBody, signature)).isFalse();
    }

    @Test
    void shouldRejectWrongSecret() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = PayloadIntegrity.sign(SECRET, timestamp, BODY);

        assertThat(PayloadIntegrity.verify("wrong-secret-key-not-matching!!", timestamp, BODY, signature)).isFalse();
    }

    @Test
    void shouldRejectReplayedRequest() {
        String oldTimestamp = String.valueOf(Instant.now().minus(Duration.ofMinutes(10)).getEpochSecond());
        String signature = PayloadIntegrity.sign(SECRET, oldTimestamp, BODY);

        assertThat(PayloadIntegrity.verify(SECRET, oldTimestamp, BODY, signature)).isFalse();
    }

    @Test
    void shouldAcceptRequestWithinWindow() {
        String recentTimestamp = String.valueOf(Instant.now().minus(Duration.ofMinutes(2)).getEpochSecond());
        String signature = PayloadIntegrity.sign(SECRET, recentTimestamp, BODY);

        assertThat(PayloadIntegrity.verify(SECRET, recentTimestamp, BODY, signature)).isTrue();
    }

    @Test
    void shouldRejectInvalidTimestamp() {
        String signature = PayloadIntegrity.sign(SECRET, "not-a-number", BODY);

        assertThat(PayloadIntegrity.verify(SECRET, "not-a-number", BODY, signature)).isFalse();
    }

    @Test
    void shouldProduceDeterministicSignature() {
        String timestamp = "1700000000";
        String sig1 = PayloadIntegrity.sign(SECRET, timestamp, BODY);
        String sig2 = PayloadIntegrity.sign(SECRET, timestamp, BODY);

        assertThat(sig1).isEqualTo(sig2);
    }

    @Test
    void shouldProduceDifferentSignaturesForDifferentTimestamps() {
        String sig1 = PayloadIntegrity.sign(SECRET, "1700000000", BODY);
        String sig2 = PayloadIntegrity.sign(SECRET, "1700000001", BODY);

        assertThat(sig1).isNotEqualTo(sig2);
    }
}
