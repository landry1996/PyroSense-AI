package com.pyrosense.shared.id;

import com.pyrosense.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record DeviceId(UUID value) implements ValueObject {

    public DeviceId {
        Objects.requireNonNull(value, "DeviceId value must not be null");
    }

    public static DeviceId generate() {
        return new DeviceId(UUID.randomUUID());
    }

    public static DeviceId from(String uuid) {
        return new DeviceId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
