package com.pyrosense.ingestion.domain.model.dataset;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class PseudonymizationService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secretKey;

    public PseudonymizationService(String secret) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String pseudonymize(String identifier) {
        if (identifier == null) return null;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(identifier.getBytes(StandardCharsets.UTF_8));
            return "ps_" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException("Pseudonymization failed", e);
        }
    }

    public String pseudonymizeDevice(String deviceId) {
        return pseudonymize("device:" + deviceId);
    }

    public String pseudonymizeTenant(String tenantId) {
        return pseudonymize("tenant:" + tenantId);
    }
}
