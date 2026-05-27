package com.pyrosense.ingestion.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Mvp3ObservabilityConfigTest {

    private MeterRegistry registry;
    private Mvp3ObservabilityConfig.Mvp3Metrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new Mvp3ObservabilityConfig.Mvp3Metrics(registry);
    }

    @Test
    void counters_shouldBeRegistered() {
        assertThat(registry.find("pyrosense_mvp3_real_device_telemetry_received_total").counter()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_signature_invalid_total").counter()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_replay_detected_total").counter()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_low_signal_quality_total").counter()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_clock_drift_total").counter()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_offline_queue_flush_total").counter()).isNotNull();
    }

    @Test
    void timer_shouldBeRegistered() {
        assertThat(registry.find("pyrosense_mvp3_validation_duration_seconds").timer()).isNotNull();
    }

    @Test
    void gauges_shouldBeRegistered() {
        assertThat(registry.find("pyrosense_mvp3_device_offline_duration_seconds").gauge()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_signal_quality_score").gauge()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_data_quality_score").gauge()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_firmware_version_count").gauge()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_pilot_active_devices").gauge()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_pilot_data_quality_average").gauge()).isNotNull();
        assertThat(registry.find("pyrosense_mvp3_pilot_incidents_total").gauge()).isNotNull();
    }

    @Test
    void realDeviceCounter_shouldIncrement() {
        metrics.realDeviceTelemetryReceived().increment();
        metrics.realDeviceTelemetryReceived().increment();
        assertThat(metrics.realDeviceTelemetryReceived().count()).isEqualTo(2.0);
    }

    @Test
    void signatureInvalidCounter_shouldIncrement() {
        metrics.signatureInvalid().increment();
        assertThat(metrics.signatureInvalid().count()).isEqualTo(1.0);
    }

    @Test
    void replayDetectedCounter_shouldIncrement() {
        metrics.replayDetected().increment();
        metrics.replayDetected().increment();
        metrics.replayDetected().increment();
        assertThat(metrics.replayDetected().count()).isEqualTo(3.0);
    }

    @Test
    void offlineDuration_shouldTrackMaximum() {
        metrics.recordOfflineDuration(100);
        assertThat(registry.find("pyrosense_mvp3_device_offline_duration_seconds").gauge().value()).isEqualTo(100.0);

        metrics.recordOfflineDuration(50);
        assertThat(registry.find("pyrosense_mvp3_device_offline_duration_seconds").gauge().value()).isEqualTo(100.0);

        metrics.recordOfflineDuration(200);
        assertThat(registry.find("pyrosense_mvp3_device_offline_duration_seconds").gauge().value()).isEqualTo(200.0);
    }

    @Test
    void signalQuality_shouldUpdateLatestValue() {
        metrics.recordSignalQuality(85);
        assertThat(registry.find("pyrosense_mvp3_signal_quality_score").gauge().value()).isEqualTo(85.0);

        metrics.recordSignalQuality(72);
        assertThat(registry.find("pyrosense_mvp3_signal_quality_score").gauge().value()).isEqualTo(72.0);
    }

    @Test
    void dataQuality_shouldUpdateLatestValue() {
        metrics.recordDataQuality(92);
        assertThat(registry.find("pyrosense_mvp3_data_quality_score").gauge().value()).isEqualTo(92.0);
    }

    @Test
    void pilotMetrics_shouldUpdate() {
        metrics.updatePilotActiveDevices(8);
        metrics.updatePilotDataQualityAverage(88);
        metrics.updatePilotIncidents(3);
        metrics.updateFirmwareVersionCount(2);

        assertThat(registry.find("pyrosense_mvp3_pilot_active_devices").gauge().value()).isEqualTo(8.0);
        assertThat(registry.find("pyrosense_mvp3_pilot_data_quality_average").gauge().value()).isEqualTo(88.0);
        assertThat(registry.find("pyrosense_mvp3_pilot_incidents_total").gauge().value()).isEqualTo(3.0);
        assertThat(registry.find("pyrosense_mvp3_firmware_version_count").gauge().value()).isEqualTo(2.0);
    }

    @Test
    void validationTimer_shouldRecordDuration() {
        metrics.validationDuration().record(java.time.Duration.ofMillis(150));
        assertThat(metrics.validationDuration().count()).isEqualTo(1);
        assertThat(metrics.validationDuration().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isCloseTo(150.0, org.assertj.core.data.Offset.offset(1.0));
    }

    @Test
    void counterLabels_shouldBeCorrect() {
        var counter = registry.find("pyrosense_mvp3_real_device_telemetry_received_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.getId().getTag("source")).isEqualTo("mqtt_v1");
    }

    @Test
    void metricNames_shouldNotContainSecrets() {
        registry.getMeters().forEach(meter -> {
            String name = meter.getId().getName();
            assertThat(name).doesNotContainIgnoringCase("secret");
            assertThat(name).doesNotContainIgnoringCase("key");
            assertThat(name).doesNotContainIgnoringCase("password");
            assertThat(name).doesNotContainIgnoringCase("token");
            assertThat(name).doesNotContainIgnoringCase("credential");

            meter.getId().getTags().forEach(tag -> {
                assertThat(tag.getValue()).doesNotContainIgnoringCase("secret");
                assertThat(tag.getValue()).doesNotContainIgnoringCase("password");
            });
        });
    }

    @Test
    void allFourteenMetrics_shouldBeRegistered() {
        long metricCount = registry.getMeters().stream()
                .filter(m -> m.getId().getName().startsWith("pyrosense_mvp3_"))
                .map(m -> m.getId().getName())
                .distinct()
                .count();
        assertThat(metricCount).isEqualTo(14);
    }
}
