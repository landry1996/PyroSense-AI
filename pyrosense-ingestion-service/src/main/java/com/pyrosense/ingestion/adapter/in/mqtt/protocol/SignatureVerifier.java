package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class SignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";

    public enum VerificationResult {
        VALID,
        INVALID_SIGNATURE,
        SIGNATURE_DISABLED,
        MISSING_KEY
    }

    public VerificationResult verify(String rawPayload, String signature, byte[] hmacKey) {
        if (hmacKey == null || hmacKey.length == 0) {
            return VerificationResult.SIGNATURE_DISABLED;
        }

        if (signature == null || signature.isBlank() || "disabled".equals(signature)) {
            return VerificationResult.INVALID_SIGNATURE;
        }

        String payloadToSign = removeSignatureValue(rawPayload);
        String expectedSignature = computeHmac(payloadToSign, hmacKey);

        if (expectedSignature == null) {
            return VerificationResult.MISSING_KEY;
        }

        return constantTimeEquals(expectedSignature, signature) ?
                VerificationResult.VALID : VerificationResult.INVALID_SIGNATURE;
    }

    public String computeHmac(String payload, byte[] key) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return null;
        }
    }

    private String removeSignatureValue(String json) {
        return json.replaceFirst("\"signature\":\"[^\"]*\"", "\"signature\":\"\"");
    }

    private boolean constantTimeEquals(String a, String b) {
        int result = a.length() ^ b.length();
        int limit = Math.min(a.length(), b.length());
        for (int i = 0; i < limit; i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
