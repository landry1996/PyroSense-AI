package com.pyrosense.ingestion.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class IngestionRejectionReasonTest {

    @ParameterizedTest
    @CsvSource({
            "INVALID_SIGNATURE,INVALID_SIGNATURE",
            "REPLAY_DETECTED,REPLAY_DETECTED",
            "NONCE_REUSED,REPLAY_DETECTED",
            "DUPLICATE_MESSAGE,REPLAY_DETECTED",
            "SEQUENCE_REGRESSION,REPLAY_DETECTED",
            "DEVICE_REVOKED,DEVICE_REVOKED",
            "DEVICE_NOT_FOUND,UNKNOWN_DEVICE",
            "INVALID_SCHEMA_VERSION,INVALID_SCHEMA_VERSION",
            "TIMESTAMP_TOO_OLD,PAYLOAD_TOO_OLD",
            "TIMESTAMP_FUTURE,PAYLOAD_TOO_OLD",
            "FIELD_OUT_OF_RANGE,INVALID_MEASUREMENT_RANGE",
            "TOPIC_PAYLOAD_MISMATCH,INVALID_MEASUREMENT_RANGE"
    })
    @DisplayName("fromPipelineCode maps all known codes correctly")
    void mappings(String pipelineCode, String expectedReason) {
        assertThat(IngestionRejectionReason.fromPipelineCode(pipelineCode))
                .isEqualTo(IngestionRejectionReason.valueOf(expectedReason));
    }

    @Test
    @DisplayName("unknown codes default to INVALID_MEASUREMENT_RANGE")
    void unknownCode() {
        assertThat(IngestionRejectionReason.fromPipelineCode("UNKNOWN_CODE"))
                .isEqualTo(IngestionRejectionReason.INVALID_MEASUREMENT_RANGE);
    }

    @Test
    @DisplayName("all enum values have descriptions")
    void allHaveDescriptions() {
        for (var reason : IngestionRejectionReason.values()) {
            assertThat(reason.getDescription()).isNotBlank();
        }
    }
}
