package com.pyrosense.device.domain.model;

import com.pyrosense.shared.id.DeviceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DeviceCredentialTest {

    private static final DeviceId DEVICE_ID = DeviceId.generate();

    @Test
    @DisplayName("Issue creates active credential with HMAC key")
    void issueCreatesActiveCredential() {
        var pair = DeviceCredential.issue(DEVICE_ID, 1);

        assertThat(pair.plainHmacKey()).isNotBlank();
        assertThat(pair.plainHmacKey().length()).isGreaterThan(20);
        assertThat(pair.credential().getDeviceId()).isEqualTo(DEVICE_ID);
        assertThat(pair.credential().getStatus()).isEqualTo(CredentialStatus.ACTIVE);
        assertThat(pair.credential().isActive()).isTrue();
        assertThat(pair.credential().getVersion()).isEqualTo(1);
        assertThat(pair.credential().getHmacKeyHash()).isNotBlank();
    }

    @Test
    @DisplayName("Revoke changes status to REVOKED")
    void revokeChangesStatus() {
        var pair = DeviceCredential.issue(DEVICE_ID, 1);
        pair.credential().revoke();

        assertThat(pair.credential().getStatus()).isEqualTo(CredentialStatus.REVOKED);
        assertThat(pair.credential().isActive()).isFalse();
        assertThat(pair.credential().getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("Revoking already revoked credential is idempotent")
    void revokeIdempotent() {
        var pair = DeviceCredential.issue(DEVICE_ID, 1);
        pair.credential().revoke();
        var firstRevokeTime = pair.credential().getRevokedAt();

        pair.credential().revoke();

        assertThat(pair.credential().getRevokedAt()).isEqualTo(firstRevokeTime);
    }

    @Test
    @DisplayName("Each issuance produces a unique HMAC key")
    void uniqueKeys() {
        var pair1 = DeviceCredential.issue(DEVICE_ID, 1);
        var pair2 = DeviceCredential.issue(DEVICE_ID, 2);

        assertThat(pair1.plainHmacKey()).isNotEqualTo(pair2.plainHmacKey());
        assertThat(pair1.credential().getHmacKeyHash())
                .isNotEqualTo(pair2.credential().getHmacKeyHash());
    }

    @Test
    @DisplayName("Version is tracked correctly")
    void versionTracked() {
        var pair1 = DeviceCredential.issue(DEVICE_ID, 1);
        var pair2 = DeviceCredential.issue(DEVICE_ID, 2);
        var pair3 = DeviceCredential.issue(DEVICE_ID, 3);

        assertThat(pair1.credential().getVersion()).isEqualTo(1);
        assertThat(pair2.credential().getVersion()).isEqualTo(2);
        assertThat(pair3.credential().getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("HMAC key hash is a SHA-256 base64 string")
    void hmacKeyHashIsSha256() {
        var pair = DeviceCredential.issue(DEVICE_ID, 1);
        // SHA-256 produces 32 bytes = 44 chars in base64
        assertThat(pair.credential().getHmacKeyHash()).hasSize(44);
    }
}
