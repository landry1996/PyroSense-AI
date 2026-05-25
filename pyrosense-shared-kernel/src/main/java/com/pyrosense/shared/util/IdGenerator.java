package com.pyrosense.shared.util;

import java.util.UUID;

/**
 * Centralized ID generation. Uses UUID v4 by default.
 * Can be extended to support UUIDv7 (time-ordered) in the future.
 */
public final class IdGenerator {

    private IdGenerator() {}

    public static UUID generate() {
        return UUID.randomUUID();
    }

    public static String generateAsString() {
        return UUID.randomUUID().toString();
    }
}
