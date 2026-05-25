package com.pyrosense.device.adapter.out.security;

import com.pyrosense.device.application.port.out.EnrollmentKeyGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class HmacEnrollmentKeyGenerator implements EnrollmentKeyGeneratorPort {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int KEY_LENGTH_BYTES = 32;

    @Override
    public EnrollmentKeyPair generate() {
        byte[] keyBytes = new byte[KEY_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(keyBytes);
        String plainKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        String keyHash = sha256(plainKey);
        return new EnrollmentKeyPair(plainKey, keyHash);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
