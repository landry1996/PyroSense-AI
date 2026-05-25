package com.pyrosense.shared.id;

import com.pyrosense.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record CircuitId(UUID value) implements ValueObject {

    public CircuitId {
        Objects.requireNonNull(value, "CircuitId value must not be null");
    }

    public static CircuitId generate() {
        return new CircuitId(UUID.randomUUID());
    }

    public static CircuitId from(String uuid) {
        return new CircuitId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
