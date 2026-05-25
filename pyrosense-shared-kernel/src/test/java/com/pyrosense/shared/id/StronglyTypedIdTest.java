package com.pyrosense.shared.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StronglyTypedIdTest {

    @Test
    void shouldGenerateUniqueIds() {
        var id1 = DeviceId.generate();
        var id2 = DeviceId.generate();
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1.value()).isNotNull();
    }

    @Test
    void shouldCreateFromString() {
        var uuid = UUID.randomUUID();
        var id = AlertId.from(uuid.toString());
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void shouldRejectNullValue() {
        assertThatThrownBy(() -> new TenantId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("TenantId");

        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("UserId");

        assertThatThrownBy(() -> new DeviceId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("DeviceId");

        assertThatThrownBy(() -> new BuildingId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("BuildingId");

        assertThatThrownBy(() -> new ElectricalPanelId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ElectricalPanelId");

        assertThatThrownBy(() -> new CircuitId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("CircuitId");

        assertThatThrownBy(() -> new AlertId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("AlertId");

        assertThatThrownBy(() -> new RiskAssessmentId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("RiskAssessmentId");
    }

    @Test
    void shouldRejectInvalidUuidString() {
        assertThatThrownBy(() -> DeviceId.from("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHaveValueEquality() {
        var uuid = UUID.randomUUID();
        var id1 = new DeviceId(uuid);
        var id2 = new DeviceId(uuid);
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void shouldNotEqualDifferentIdTypes() {
        var uuid = UUID.randomUUID();
        var deviceId = new DeviceId(uuid);
        var alertId = new AlertId(uuid);
        assertThat(deviceId).isNotEqualTo(alertId);
    }

    @Test
    void toStringShouldReturnUuidString() {
        var uuid = UUID.randomUUID();
        var id = new BuildingId(uuid);
        assertThat(id.toString()).isEqualTo(uuid.toString());
    }
}
