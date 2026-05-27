package com.pyrosense.ingestion.application.port.out;

public interface DeviceSignatureVerificationPort {

    enum VerificationResult {
        VALID,
        INVALID,
        KEY_NOT_FOUND
    }

    VerificationResult verifySignature(String rawPayload, String signature, byte[] hmacKey);
}
