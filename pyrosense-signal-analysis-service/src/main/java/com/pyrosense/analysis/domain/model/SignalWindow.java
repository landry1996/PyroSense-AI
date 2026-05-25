package com.pyrosense.analysis.domain.model;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.*;

public final class SignalWindow {

    private final DeviceId deviceId;
    private final TenantId tenantId;
    private final List<SignalSample> samples;

    public SignalWindow(DeviceId deviceId, TenantId tenantId, List<SignalSample> samples) {
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(tenantId);
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("Signal window must contain at least one sample");
        }
        this.deviceId = deviceId;
        this.tenantId = tenantId;
        this.samples = List.copyOf(samples);
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public List<SignalSample> samples() {
        return samples;
    }

    public int size() {
        return samples.size();
    }

    public Instant startTime() {
        return samples.getFirst().timestamp();
    }

    public Instant endTime() {
        return samples.getLast().timestamp();
    }

    public double[] valuesFor(SignalFeature feature) {
        return samples.stream()
                .mapToDouble(s -> s.valueFor(feature))
                .toArray();
    }

    public double latestValueFor(SignalFeature feature) {
        return samples.getLast().valueFor(feature);
    }

    public record SignalSample(
            Instant timestamp,
            Map<SignalFeature, Double> values
    ) {
        public SignalSample {
            Objects.requireNonNull(timestamp);
            values = Map.copyOf(values);
        }

        public double valueFor(SignalFeature feature) {
            return values.getOrDefault(feature, 0.0);
        }
    }
}
