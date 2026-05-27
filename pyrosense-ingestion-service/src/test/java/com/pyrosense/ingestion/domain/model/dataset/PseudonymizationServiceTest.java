package com.pyrosense.ingestion.domain.model.dataset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PseudonymizationServiceTest {

    private final PseudonymizationService service = new PseudonymizationService("test-secret-key");

    @Test
    @DisplayName("pseudonymize produces deterministic output")
    void deterministic() {
        String result1 = service.pseudonymize("device-001");
        String result2 = service.pseudonymize("device-001");

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("different inputs produce different outputs")
    void differentInputs() {
        String result1 = service.pseudonymize("device-001");
        String result2 = service.pseudonymize("device-002");

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("output is prefixed with ps_")
    void prefixed() {
        String result = service.pseudonymize("anything");

        assertThat(result).startsWith("ps_");
        assertThat(result).hasSize(19); // ps_ + 16 hex chars
    }

    @Test
    @DisplayName("different keys produce different pseudonyms")
    void differentKeys() {
        var service2 = new PseudonymizationService("different-key");

        String result1 = service.pseudonymize("device-001");
        String result2 = service2.pseudonymize("device-001");

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("pseudonymizeDevice adds device prefix before hashing")
    void devicePrefix() {
        String direct = service.pseudonymize("device:device-001");
        String viaMethod = service.pseudonymizeDevice("device-001");

        assertThat(direct).isEqualTo(viaMethod);
    }

    @Test
    @DisplayName("pseudonymizeTenant adds tenant prefix before hashing")
    void tenantPrefix() {
        String direct = service.pseudonymize("tenant:tenant-001");
        String viaMethod = service.pseudonymizeTenant("tenant-001");

        assertThat(direct).isEqualTo(viaMethod);
    }

    @Test
    @DisplayName("null input returns null")
    void nullInput() {
        assertThat(service.pseudonymize(null)).isNull();
    }
}
