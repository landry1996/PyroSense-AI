package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DeviceRateLimiter {

    public enum RateLimitResult { ALLOWED, RATE_LIMITED }

    private final int maxMessagesPerWindow;
    private final int windowSeconds;
    private final Map<String, DeviceWindow> windows = new ConcurrentHashMap<>();

    public DeviceRateLimiter(int maxMessagesPerWindow, int windowSeconds) {
        this.maxMessagesPerWindow = maxMessagesPerWindow;
        this.windowSeconds = windowSeconds;
    }

    public RateLimitResult check(String deviceId) {
        var window = windows.computeIfAbsent(deviceId, k -> new DeviceWindow());
        long now = Instant.now().getEpochSecond();

        synchronized (window) {
            long windowStart = window.windowStart.get();
            if (now - windowStart >= windowSeconds) {
                window.windowStart.set(now);
                window.count.set(1);
                return RateLimitResult.ALLOWED;
            }
            int current = window.count.incrementAndGet();
            return current <= maxMessagesPerWindow ? RateLimitResult.ALLOWED : RateLimitResult.RATE_LIMITED;
        }
    }

    public void reset(String deviceId) {
        windows.remove(deviceId);
    }

    private static class DeviceWindow {
        final AtomicLong windowStart = new AtomicLong(Instant.now().getEpochSecond());
        final AtomicInteger count = new AtomicInteger(0);
    }
}
