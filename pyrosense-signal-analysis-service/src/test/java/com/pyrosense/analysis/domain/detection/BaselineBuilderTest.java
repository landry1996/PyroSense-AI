package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.BaselineProfile;
import com.pyrosense.analysis.domain.model.SignalFeature;
import com.pyrosense.analysis.domain.model.SignalWindow;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class BaselineBuilderTest {

    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");

    @Test
    void shouldBuildBaselineAfterMinimumSamples() {
        var builder = new BaselineBuilder(deviceId, 5);

        for (int i = 0; i < 5; i++) {
            builder.addSample(new SignalWindow.SignalSample(now.plusSeconds(i),
                    Map.of(SignalFeature.THD, 4.0 + i * 0.5)));
        }

        assertThat(builder.isReady()).isTrue();
        BaselineProfile profile = builder.build(now);
        assertThat(profile.isReady()).isTrue();
        assertThat(profile.sampleCount()).isEqualTo(5);
        assertThat(profile.hasFeature(SignalFeature.THD)).isTrue();
    }

    @Test
    void shouldNotBeReadyBelowMinimum() {
        var builder = new BaselineBuilder(deviceId, 100);
        builder.addSample(new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 5.0)));

        assertThat(builder.isReady()).isFalse();
    }

    @Test
    void shouldComputeCorrectMean() {
        var builder = new BaselineBuilder(deviceId, 3);
        builder.addSample(new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 4.0)));
        builder.addSample(new SignalWindow.SignalSample(now.plusSeconds(1), Map.of(SignalFeature.THD, 6.0)));
        builder.addSample(new SignalWindow.SignalSample(now.plusSeconds(2), Map.of(SignalFeature.THD, 8.0)));

        BaselineProfile profile = builder.build(now);
        assertThat(profile.statsFor(SignalFeature.THD).mean()).isEqualTo(6.0);
    }

    @Test
    void shouldComputePercentiles() {
        var builder = new BaselineBuilder(deviceId, 10);
        for (int i = 1; i <= 20; i++) {
            builder.addSample(new SignalWindow.SignalSample(now.plusSeconds(i),
                    Map.of(SignalFeature.TEMPERATURE, (double) i)));
        }

        BaselineProfile profile = builder.build(now);
        var stats = profile.statsFor(SignalFeature.TEMPERATURE);
        assertThat(stats.p50()).isCloseTo(10.0, within(1.0));
        assertThat(stats.min()).isEqualTo(1.0);
        assertThat(stats.max()).isEqualTo(20.0);
    }

    @Test
    void shouldAddWindowBatch() {
        var builder = new BaselineBuilder(deviceId, 3);
        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        samples.add(new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 4.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(1), Map.of(SignalFeature.THD, 5.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(2), Map.of(SignalFeature.THD, 6.0)));

        var window = new SignalWindow(deviceId, tenantId, samples);
        builder.addWindow(window);

        assertThat(builder.isReady()).isTrue();
        assertThat(builder.sampleCount()).isEqualTo(3);
    }

    @Test
    void shouldComputeStdDev() {
        var builder = new BaselineBuilder(deviceId, 4);
        double[] values = {2, 4, 4, 4, 5, 5, 7, 9};
        for (int i = 0; i < values.length; i++) {
            builder.addSample(new SignalWindow.SignalSample(now.plusSeconds(i),
                    Map.of(SignalFeature.RMS_CURRENT, values[i])));
        }

        BaselineProfile profile = builder.build(now);
        var stats = profile.statsFor(SignalFeature.RMS_CURRENT);
        assertThat(stats.mean()).isEqualTo(5.0);
        assertThat(stats.stdDev()).isCloseTo(2.0, within(0.15));
    }
}
