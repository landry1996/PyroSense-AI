package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

public class AntiReplayGuard {

    public enum ReplayCheckResult {
        OK,
        NONCE_REUSED,
        SEQUENCE_REGRESSION,
        DUPLICATE_MESSAGE
    }

    private final NonceStore nonceStore;
    private final SequenceStore sequenceStore;
    private final MessageIdStore messageIdStore;

    public AntiReplayGuard(NonceStore nonceStore, SequenceStore sequenceStore, MessageIdStore messageIdStore) {
        this.nonceStore = nonceStore;
        this.sequenceStore = sequenceStore;
        this.messageIdStore = messageIdStore;
    }

    public ReplayCheckResult check(String deviceId, String messageId, String nonce, long sequenceNumber) {
        // 1. Check messageId uniqueness (idempotency)
        if (messageIdStore.exists(messageId)) {
            return ReplayCheckResult.DUPLICATE_MESSAGE;
        }

        // 2. Check nonce uniqueness (anti-replay)
        if (nonceStore.exists(nonce)) {
            return ReplayCheckResult.NONCE_REUSED;
        }

        // 3. Check sequence monotonicity
        long lastSequence = sequenceStore.getLastSequence(deviceId);
        if (sequenceNumber > 0 && lastSequence > 0 && sequenceNumber <= lastSequence) {
            return ReplayCheckResult.SEQUENCE_REGRESSION;
        }

        // All checks passed — record the values
        nonceStore.store(nonce);
        sequenceStore.updateSequence(deviceId, sequenceNumber);
        messageIdStore.store(messageId);

        return ReplayCheckResult.OK;
    }

    public interface NonceStore {
        boolean exists(String nonce);
        void store(String nonce);
    }

    public interface SequenceStore {
        long getLastSequence(String deviceId);
        void updateSequence(String deviceId, long sequence);
    }

    public interface MessageIdStore {
        boolean exists(String messageId);
        void store(String messageId);
    }
}
