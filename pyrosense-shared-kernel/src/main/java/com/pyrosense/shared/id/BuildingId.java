package com.pyrosense.shared.id;

import com.pyrosense.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record BuildingId(UUID value) implements ValueObject {

    public BuildingId {
        Objects.requireNonNull(value, "BuildingId value must not be null");
    }

    public static BuildingId generate() {
        return new BuildingId(UUID.randomUUID());
    }

    public static BuildingId from(String uuid) {
        return new BuildingId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
