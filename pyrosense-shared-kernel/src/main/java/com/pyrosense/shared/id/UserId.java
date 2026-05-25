package com.pyrosense.shared.id;

import com.pyrosense.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) implements ValueObject {

    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId from(String uuid) {
        return new UserId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
