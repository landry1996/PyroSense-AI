package com.pyrosense.shared.id;

import com.pyrosense.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record TenantId(UUID value) implements ValueObject {

    public TenantId {
        Objects.requireNonNull(value, "TenantId value must not be null");
    }

    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }

    public static TenantId from(String uuid) {
        return new TenantId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
