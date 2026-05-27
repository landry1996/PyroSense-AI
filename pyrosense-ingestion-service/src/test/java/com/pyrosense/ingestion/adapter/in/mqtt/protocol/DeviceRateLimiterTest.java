package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeviceRateLimiter")
class DeviceRateLimiterTest {

    private DeviceRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new DeviceRateLimiter(10, 60);
    }

    @Test
    @DisplayName("First message is always allowed")
    void firstMessageAllowed() {
        assertThat(limiter.check("dev-001"))
                .isEqualTo(DeviceRateLimiter.RateLimitResult.ALLOWED);
    }

    @Test
    @DisplayName("Messages within limit are allowed")
    void withinLimitAllowed() {
        for (int i = 0; i < 10; i++) {
            assertThat(limiter.check("dev-001"))
                    .isEqualTo(DeviceRateLimiter.RateLimitResult.ALLOWED);
        }
    }

    @Test
    @DisplayName("Message exceeding limit is rejected")
    void exceedingLimitRejected() {
        for (int i = 0; i < 10; i++) {
            limiter.check("dev-001");
        }
        assertThat(limiter.check("dev-001"))
                .isEqualTo(DeviceRateLimiter.RateLimitResult.RATE_LIMITED);
    }

    @Test
    @DisplayName("Different devices have independent windows")
    void independentDevices() {
        for (int i = 0; i < 10; i++) {
            limiter.check("dev-001");
        }
        assertThat(limiter.check("dev-001"))
                .isEqualTo(DeviceRateLimiter.RateLimitResult.RATE_LIMITED);
        assertThat(limiter.check("dev-002"))
                .isEqualTo(DeviceRateLimiter.RateLimitResult.ALLOWED);
    }

    @Test
    @DisplayName("Reset clears device window")
    void resetClearsWindow() {
        for (int i = 0; i < 10; i++) {
            limiter.check("dev-001");
        }
        assertThat(limiter.check("dev-001"))
                .isEqualTo(DeviceRateLimiter.RateLimitResult.RATE_LIMITED);

        limiter.reset("dev-001");
        assertThat(limiter.check("dev-001"))
                .isEqualTo(DeviceRateLimiter.RateLimitResult.ALLOWED);
    }

    @Test
    @DisplayName("Zero-limit limiter rejects all messages")
    void zeroLimitRejectsAll() {
        var strictLimiter = new DeviceRateLimiter(0, 60);
        assertThat(strictLimiter.check("dev-001"))
                .isEqualTo(DeviceRateLimiter.RateLimitResult.RATE_LIMITED);
    }
}
