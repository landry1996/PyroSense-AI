package com.pyrosense.identity.domain.model;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    LOCKED;

    public boolean canAuthenticate() {
        return this == ACTIVE;
    }
}
