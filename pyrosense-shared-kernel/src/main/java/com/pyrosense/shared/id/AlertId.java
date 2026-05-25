package com.pyrosense.shared.id;

import com.pyrosense.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record AlertId(UUID value) implements ValueObject {

    public AlertId {
        Objects.requireNonNull(value, "AlertId value must not be null");
    }

    public static AlertId generate() {
        return new AlertId(UUID.randomUUID());
    }

    public static AlertId from(String uuid) {
        return new AlertId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
