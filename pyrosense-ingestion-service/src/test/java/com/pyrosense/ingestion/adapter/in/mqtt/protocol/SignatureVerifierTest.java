package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureVerifierTest {

    private SignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new SignatureVerifier();
    }

    @Test
    @DisplayName("Valid signature is accepted")
    void validSignature() {
        byte[] key = "test-key-32-bytes-long-enough!!!".getBytes(StandardCharsets.UTF_8);
        String payload = "{\"data\":\"test\",\"security\":{\"nonce\":\"abc\",\"signature\":\"\"}}";
        String signature = verifier.computeHmac(payload, key);

        String fullPayload = payload.replace("\"signature\":\"\"", "\"signature\":\"" + signature + "\"");
        var result = verifier.verify(fullPayload, signature, key);
        assertThat(result).isEqualTo(SignatureVerifier.VerificationResult.VALID);
    }

    @Test
    @DisplayName("Invalid signature is rejected")
    void invalidSignature() {
        byte[] key = "test-key-32-bytes-long-enough!!!".getBytes(StandardCharsets.UTF_8);
        String payload = "{\"data\":\"test\",\"security\":{\"nonce\":\"abc\",\"signature\":\"wrong-sig\"}}";

        var result = verifier.verify(payload, "wrong-sig", key);
        assertThat(result).isEqualTo(SignatureVerifier.VerificationResult.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("Null key means signature disabled")
    void nullKeyDisabled() {
        var result = verifier.verify("{}", "some-sig", null);
        assertThat(result).isEqualTo(SignatureVerifier.VerificationResult.SIGNATURE_DISABLED);
    }

    @Test
    @DisplayName("Empty key means signature disabled")
    void emptyKeyDisabled() {
        var result = verifier.verify("{}", "some-sig", new byte[0]);
        assertThat(result).isEqualTo(SignatureVerifier.VerificationResult.SIGNATURE_DISABLED);
    }

    @Test
    @DisplayName("'disabled' signature string is rejected when key exists")
    void disabledStringRejected() {
        byte[] key = "my-secret-key-for-hmac-signing!!".getBytes(StandardCharsets.UTF_8);
        var result = verifier.verify("{}", "disabled", key);
        assertThat(result).isEqualTo(SignatureVerifier.VerificationResult.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("Tampered payload fails verification")
    void tamperedPayload() {
        byte[] key = "test-key-32-bytes-long-enough!!!".getBytes(StandardCharsets.UTF_8);
        String original = "{\"value\":10,\"security\":{\"nonce\":\"abc\",\"signature\":\"\"}}";
        String signature = verifier.computeHmac(original, key);

        String tampered = "{\"value\":99,\"security\":{\"nonce\":\"abc\",\"signature\":\"" + signature + "\"}}";
        var result = verifier.verify(tampered, signature, key);
        assertThat(result).isEqualTo(SignatureVerifier.VerificationResult.INVALID_SIGNATURE);
    }
}
