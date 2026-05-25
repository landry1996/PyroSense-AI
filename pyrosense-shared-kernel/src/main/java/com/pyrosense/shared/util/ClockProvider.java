package com.pyrosense.shared.util;

import java.time.Clock;
import java.time.Instant;

/**
 * Abstraction over system clock to enable deterministic testing.
 * In production: uses Clock.systemUTC().
 * In tests: inject a fixed clock.
 */
public final class ClockProvider {

    private static Clock clock = Clock.systemUTC();

    private ClockProvider() {}

    public static Instant now() {
        return Instant.now(clock);
    }

    public static Clock getClock() {
        return clock;
    }

    /**
     * Override the clock (for testing only).
     */
    public static void setClock(Clock testClock) {
        clock = testClock;
    }

    /**
     * Reset to system clock.
     */
    public static void reset() {
        clock = Clock.systemUTC();
    }
}
