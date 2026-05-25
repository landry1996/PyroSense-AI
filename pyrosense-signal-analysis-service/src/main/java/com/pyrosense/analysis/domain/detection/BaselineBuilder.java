package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.*;

public final class BaselineBuilder {

    private final DeviceId deviceId;
    private final int minimumSamples;
    private final Map<SignalFeature, RunningStats> runningStats = new EnumMap<>(SignalFeature.class);
    private int sampleCount = 0;

    public BaselineBuilder(DeviceId deviceId, int minimumSamples) {
        this.deviceId = Objects.requireNonNull(deviceId);
        this.minimumSamples = minimumSamples;
        for (SignalFeature f : SignalFeature.values()) {
            runningStats.put(f, new RunningStats());
        }
    }

    public void addSample(SignalWindow.SignalSample sample) {
        for (SignalFeature feature : SignalFeature.values()) {
            double value = sample.valueFor(feature);
            runningStats.get(feature).add(value);
        }
        sampleCount++;
    }

    public void addWindow(SignalWindow window) {
        for (var sample : window.samples()) {
            addSample(sample);
        }
    }

    public boolean isReady() {
        return sampleCount >= minimumSamples;
    }

    public int sampleCount() {
        return sampleCount;
    }

    public BaselineProfile build(Instant now) {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        for (var entry : runningStats.entrySet()) {
            if (entry.getValue().count() > 0) {
                stats.put(entry.getKey(), entry.getValue().toStatisticalRange());
            }
        }
        return new BaselineProfile(UUID.randomUUID(), deviceId, stats, sampleCount, now, now, minimumSamples);
    }

    public static final class RunningStats {
        private long count = 0;
        private double mean = 0;
        private double m2 = 0;
        private double min = Double.MAX_VALUE;
        private double max = Double.MIN_VALUE;
        private final List<Double> values = new ArrayList<>();

        public void add(double value) {
            count++;
            double delta = value - mean;
            mean += delta / count;
            double delta2 = value - mean;
            m2 += delta * delta2;
            min = Math.min(min, value);
            max = Math.max(max, value);
            values.add(value);
        }

        public long count() {
            return count;
        }

        public double mean() {
            return mean;
        }

        public double stdDev() {
            if (count < 2) return 0;
            return Math.sqrt(m2 / (count - 1));
        }

        public StatisticalRange toStatisticalRange() {
            Collections.sort(values);
            return new StatisticalRange(
                    mean, stdDev(), min, max,
                    percentile(5), percentile(25), percentile(50), percentile(75), percentile(95),
                    count
            );
        }

        private double percentile(int p) {
            if (values.isEmpty()) return 0;
            int index = (int) Math.ceil(p / 100.0 * values.size()) - 1;
            return values.get(Math.max(0, Math.min(index, values.size() - 1)));
        }
    }
}
