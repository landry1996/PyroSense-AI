package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AntiReplayGuardTest {

    private AntiReplayGuard guard;
    private InMemoryNonceStore nonceStore;
    private InMemorySequenceStore sequenceStore;
    private InMemoryMessageIdStore messageIdStore;

    @BeforeEach
    void setUp() {
        nonceStore = new InMemoryNonceStore();
        sequenceStore = new InMemorySequenceStore();
        messageIdStore = new InMemoryMessageIdStore();
        guard = new AntiReplayGuard(nonceStore, sequenceStore, messageIdStore);
    }

    @Test
    @DisplayName("First message is accepted")
    void firstMessageAccepted() {
        var result = guard.check("device-001", "msg-1", "nonce-1", 1);
        assertThat(result).isEqualTo(AntiReplayGuard.ReplayCheckResult.OK);
    }

    @Test
    @DisplayName("Duplicate messageId is detected")
    void duplicateMessageId() {
        guard.check("device-001", "msg-1", "nonce-1", 1);
        var result = guard.check("device-001", "msg-1", "nonce-2", 2);
        assertThat(result).isEqualTo(AntiReplayGuard.ReplayCheckResult.DUPLICATE_MESSAGE);
    }

    @Test
    @DisplayName("Reused nonce is detected")
    void reusedNonce() {
        guard.check("device-001", "msg-1", "nonce-1", 1);
        var result = guard.check("device-001", "msg-2", "nonce-1", 2);
        assertThat(result).isEqualTo(AntiReplayGuard.ReplayCheckResult.NONCE_REUSED);
    }

    @Test
    @DisplayName("Sequence regression is detected")
    void sequenceRegression() {
        guard.check("device-001", "msg-1", "nonce-1", 10);
        var result = guard.check("device-001", "msg-2", "nonce-2", 5);
        assertThat(result).isEqualTo(AntiReplayGuard.ReplayCheckResult.SEQUENCE_REGRESSION);
    }

    @Test
    @DisplayName("Equal sequence number is detected as regression")
    void equalSequenceRejected() {
        guard.check("device-001", "msg-1", "nonce-1", 10);
        var result = guard.check("device-001", "msg-2", "nonce-2", 10);
        assertThat(result).isEqualTo(AntiReplayGuard.ReplayCheckResult.SEQUENCE_REGRESSION);
    }

    @Test
    @DisplayName("Increasing sequence is accepted")
    void increasingSequenceAccepted() {
        guard.check("device-001", "msg-1", "nonce-1", 1);
        guard.check("device-001", "msg-2", "nonce-2", 2);
        var result = guard.check("device-001", "msg-3", "nonce-3", 3);
        assertThat(result).isEqualTo(AntiReplayGuard.ReplayCheckResult.OK);
    }

    @Test
    @DisplayName("Different devices have independent sequences")
    void independentDeviceSequences() {
        guard.check("device-001", "msg-1", "nonce-1", 100);
        var result = guard.check("device-002", "msg-2", "nonce-2", 1);
        assertThat(result).isEqualTo(AntiReplayGuard.ReplayCheckResult.OK);
    }

    // --- In-memory test implementations ---

    static class InMemoryNonceStore implements AntiReplayGuard.NonceStore {
        private final Set<String> nonces = new HashSet<>();

        @Override
        public boolean exists(String nonce) { return nonces.contains(nonce); }

        @Override
        public void store(String nonce) { nonces.add(nonce); }
    }

    static class InMemorySequenceStore implements AntiReplayGuard.SequenceStore {
        private final Map<String, Long> sequences = new HashMap<>();

        @Override
        public long getLastSequence(String deviceId) { return sequences.getOrDefault(deviceId, 0L); }

        @Override
        public void updateSequence(String deviceId, long sequence) { sequences.put(deviceId, sequence); }
    }

    static class InMemoryMessageIdStore implements AntiReplayGuard.MessageIdStore {
        private final Set<String> ids = new HashSet<>();

        @Override
        public boolean exists(String messageId) { return ids.contains(messageId); }

        @Override
        public void store(String messageId) { ids.add(messageId); }
    }
}
