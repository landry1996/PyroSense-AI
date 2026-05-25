package com.pyrosense.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

/**
 * Utility for HMAC-based payload integrity and replay attack prevention.
 * Devices sign payloads with: HMAC-SHA256(secret, timestamp + "." + body)
 * The server verifies the signature and rejects requests older than the allowed window.
 */
public final class PayloadIntegrity {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Duration DEFAULT_REPLAY_WINDOW = Duration.ofMinutes(5);

    private PayloadIntegrity() {}

    public static String sign(String secret, String timestamp, String body) {
        String data = timestamp + "." + body;
        return computeHmac(secret, data);
    }

    public static boolean verify(String secret, String timestamp, String body, String signature) {
        return verify(secret, timestamp, body, signature, DEFAULT_REPLAY_WINDOW);
    }

    public static boolean verify(String secret, String timestamp, String body, String signature,
                                  Duration replayWindow) {
        Instant requestTime;
        try {
            requestTime = Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (NumberFormatException e) {
            return false;
        }

        if (Duration.between(requestTime, Instant.now()).abs().compareTo(replayWindow) > 0) {
            return false;
        }

        String expected = sign(secret, timestamp, body);
        return constantTimeEquals(expected, signature);
    }

    private static String computeHmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
