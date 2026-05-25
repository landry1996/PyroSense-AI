package com.pyrosense.alerting.domain.model;

import com.pyrosense.shared.id.DeviceId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DeduplicationKeyTest {

    @Test
    void shouldSerializeAndDeserialize() {
        DeviceId deviceId = DeviceId.generate();
        DeduplicationKey key = new DeduplicationKey(deviceId, AlertType.OVERHEATING);

        String serialized = key.toStringKey();
        DeduplicationKey deserialized = DeduplicationKey.from(serialized);

        assertThat(deserialized.deviceId()).isEqualTo(deviceId);
        assertThat(deserialized.type()).isEqualTo(AlertType.OVERHEATING);
    }

    @Test
    void shouldContainDeviceIdAndType() {
        DeviceId deviceId = DeviceId.generate();
        DeduplicationKey key = new DeduplicationKey(deviceId, AlertType.MICRO_ARC_DETECTED);

        assertThat(key.toStringKey()).contains(deviceId.value().toString());
        assertThat(key.toStringKey()).contains("MICRO_ARC_DETECTED");
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> new DeduplicationKey(null, AlertType.OVERHEATING))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DeduplicationKey(DeviceId.generate(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
