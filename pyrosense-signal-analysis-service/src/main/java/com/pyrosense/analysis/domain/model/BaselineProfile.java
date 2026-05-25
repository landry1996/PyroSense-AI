package com.pyrosense.analysis.domain.model;

import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class BaselineProfile {

    private final UUID id;
    private final DeviceId deviceId;
    private final Map<SignalFeature, StatisticalRange> featureStats;
    private final int sampleCount;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final int minimumSamplesRequired;

    public BaselineProfile(UUID id, DeviceId deviceId, Map<SignalFeature, StatisticalRange> featureStats,
                           int sampleCount, Instant createdAt, Instant updatedAt, int minimumSamplesRequired) {
        this.id = Objects.requireNonNull(id);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.featureStats = Map.copyOf(featureStats);
        this.sampleCount = sampleCount;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.minimumSamplesRequired = minimumSamplesRequired;
    }

    public UUID id() {
        return id;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public Map<SignalFeature, StatisticalRange> featureStats() {
        return featureStats;
    }

    public int sampleCount() {
        return sampleCount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean isReady() {
        return sampleCount >= minimumSamplesRequired;
    }

    public StatisticalRange statsFor(SignalFeature feature) {
        return featureStats.get(feature);
    }

    public boolean hasFeature(SignalFeature feature) {
        return featureStats.containsKey(feature);
    }
}
