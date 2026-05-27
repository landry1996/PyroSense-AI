package com.pyrosense.device.domain.model;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class ClaimTokenTest {

    private static final DeviceId DEVICE_ID = DeviceId.generate();
    private static final TenantId TENANT_ID = TenantId.generate();

    @Test
    @DisplayName("Create generates token pair with hash")
    void createGeneratesTokenPair() {
        var pair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");

        assertThat(pair.plainToken()).isNotBlank();
        assertThat(pair.plainToken().length()).isGreaterThan(20);
        assertThat(pair.claimToken()).isNotNull();
        assertThat(pair.claimToken().getDeviceId()).isEqualTo(DEVICE_ID);
        assertThat(pair.claimToken().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(pair.claimToken().isConsumed()).isFalse();
        assertThat(pair.claimToken().isValid()).isTrue();
    }

    @Test
    @DisplayName("Token matches with correct plain token")
    void matchesCorrectToken() {
        var pair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");

        assertThat(pair.claimToken().matches(pair.plainToken())).isTrue();
    }

    @Test
    @DisplayName("Token does not match incorrect plain token")
    void doesNotMatchIncorrectToken() {
        var pair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");

        assertThat(pair.claimToken().matches("wrong-token")).isFalse();
    }

    @Test
    @DisplayName("Consuming a valid token succeeds")
    void consumeSucceeds() {
        var pair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");

        pair.claimToken().consume();

        assertThat(pair.claimToken().isConsumed()).isTrue();
        assertThat(pair.claimToken().getConsumedAt()).isNotNull();
        assertThat(pair.claimToken().isValid()).isFalse();
    }

    @Test
    @DisplayName("Consuming an already consumed token throws")
    void consumeAlreadyConsumedThrows() {
        var pair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");
        pair.claimToken().consume();

        assertThatThrownBy(() -> pair.claimToken().consume())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already consumed");
    }

    @Test
    @DisplayName("Expired token is not valid")
    void expiredTokenNotValid() {
        var pair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofMillis(-1), "admin");

        assertThat(pair.claimToken().isExpired()).isTrue();
        assertThat(pair.claimToken().isValid()).isFalse();
    }

    @Test
    @DisplayName("Consuming expired token throws")
    void consumeExpiredTokenThrows() {
        var pair = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofMillis(-1), "admin");

        assertThatThrownBy(() -> pair.claimToken().consume())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Two tokens for same device have different hashes")
    void uniqueTokensPerCreation() {
        var pair1 = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");
        var pair2 = ClaimToken.create(DEVICE_ID, TENANT_ID, Duration.ofHours(24), "admin");

        assertThat(pair1.plainToken()).isNotEqualTo(pair2.plainToken());
        assertThat(pair1.claimToken().getTokenHash())
                .isNotEqualTo(pair2.claimToken().getTokenHash());
    }
}
