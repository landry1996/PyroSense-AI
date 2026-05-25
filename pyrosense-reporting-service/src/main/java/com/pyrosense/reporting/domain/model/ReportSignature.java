package com.pyrosense.reporting.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public record ReportSignature(String hash, String algorithm) {

    public ReportSignature {
        Objects.requireNonNull(hash);
        Objects.requireNonNull(algorithm);
    }

    public static ReportSignature compute(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content);
            return new ReportSignature(HexFormat.of().formatHex(hashBytes), "SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
