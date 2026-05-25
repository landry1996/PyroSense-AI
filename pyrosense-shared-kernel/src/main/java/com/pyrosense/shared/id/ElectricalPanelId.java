package com.pyrosense.shared.id;

import com.pyrosense.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record ElectricalPanelId(UUID value) implements ValueObject {

    public ElectricalPanelId {
        Objects.requireNonNull(value, "ElectricalPanelId value must not be null");
    }

    public static ElectricalPanelId generate() {
        return new ElectricalPanelId(UUID.randomUUID());
    }

    public static ElectricalPanelId from(String uuid) {
        return new ElectricalPanelId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
