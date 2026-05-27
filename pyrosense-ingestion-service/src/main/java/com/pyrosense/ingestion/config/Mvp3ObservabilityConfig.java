package com.pyrosense.ingestion.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class Mvp3ObservabilityConfig {

    @Bean
    public Mvp3Metrics mvp3Metrics(MeterRegistry registry) {
        return new Mvp3Metrics(registry);
    }

    public static class Mvp3Metrics {

        private final Counter realDeviceTelemetryReceived;
        private final Counter signatureInvalid;
        private final Counter replayDetected;
        private final Counter lowSignalQuality;
        private final Counter clockDrift;
        private final Counter offlineQueueFlush;
        private final Timer validationDuration;

        private final AtomicLong offlineDurationMax = new AtomicLong(0);
        private final AtomicInteger signalQualityLatest = new AtomicInteger(0);
        private final AtomicInteger dataQualityLatest = new AtomicInteger(0);
        private final AtomicInteger firmwareVersionCount = new AtomicInteger(0);
        private final AtomicInteger pilotActiveDevices = new AtomicInteger(0);
        private final AtomicInteger pilotDataQualityAvg = new AtomicInteger(0);
        private final AtomicInteger pilotIncidents = new AtomicInteger(0);

        public Mvp3Metrics(MeterRegistry registry) {
            this.realDeviceTelemetryReceived = Counter.builder("pyrosense_mvp3_real_device_telemetry_received_total")
                    .description("Total telemetry messages received from real MVP3 devices")
                    .tag("source", "mqtt_v1")
                    .register(registry);

            this.signatureInvalid = Counter.builder("pyrosense_mvp3_signature_invalid_total")
                    .description("Total messages with invalid HMAC-SHA256 signatures")
                    .register(registry);

            this.replayDetected = Counter.builder("pyrosense_mvp3_replay_detected_total")
                    .description("Total replay attacks detected and blocked")
                    .register(registry);

            this.lowSignalQuality = Counter.builder("pyrosense_mvp3_low_signal_quality_total")
                    .description("Total messages where signal quality is below threshold")
                    .register(registry);

            this.clockDrift = Counter.builder("pyrosense_mvp3_clock_drift_total")
                    .description("Total clock drift events detected on real devices")
                    .register(registry);

            this.offlineQueueFlush = Counter.builder("pyrosense_mvp3_offline_queue_flush_total")
                    .description("Total offline buffer flush events from devices")
                    .register(registry);

            this.validationDuration = Timer.builder("pyrosense_mvp3_validation_duration_seconds")
                    .description("Duration of full protocol validation pipeline")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(registry);

            Gauge.builder("pyrosense_mvp3_device_offline_duration_seconds", offlineDurationMax, AtomicLong::get)
                    .description("Maximum offline duration observed across devices (seconds)")
                    .register(registry);

            Gauge.builder("pyrosense_mvp3_signal_quality_score", signalQualityLatest, AtomicInteger::get)
                    .description("Latest signal quality score (0-100)")
                    .register(registry);

            Gauge.builder("pyrosense_mvp3_data_quality_score", dataQualityLatest, AtomicInteger::get)
                    .description("Latest data quality score (0-100)")
                    .register(registry);

            Gauge.builder("pyrosense_mvp3_firmware_version_count", firmwareVersionCount, AtomicInteger::get)
                    .description("Number of distinct firmware versions active in fleet")
                    .register(registry);

            Gauge.builder("pyrosense_mvp3_pilot_active_devices", pilotActiveDevices, AtomicInteger::get)
                    .description("Number of active devices in pilot program")
                    .register(registry);

            Gauge.builder("pyrosense_mvp3_pilot_data_quality_average", pilotDataQualityAvg, AtomicInteger::get)
                    .description("Average data quality score across pilot devices")
                    .register(registry);

            Gauge.builder("pyrosense_mvp3_pilot_incidents_total", pilotIncidents, AtomicInteger::get)
                    .description("Total incidents reported in pilot program")
                    .register(registry);
        }

        public Counter realDeviceTelemetryReceived() { return realDeviceTelemetryReceived; }
        public Counter signatureInvalid() { return signatureInvalid; }
        public Counter replayDetected() { return replayDetected; }
        public Counter lowSignalQuality() { return lowSignalQuality; }
        public Counter clockDrift() { return clockDrift; }
        public Counter offlineQueueFlush() { return offlineQueueFlush; }
        public Timer validationDuration() { return validationDuration; }

        public void recordOfflineDuration(long seconds) {
            offlineDurationMax.updateAndGet(current -> Math.max(current, seconds));
        }

        public void recordSignalQuality(int score) { signalQualityLatest.set(score); }
        public void recordDataQuality(int score) { dataQualityLatest.set(score); }
        public void updateFirmwareVersionCount(int count) { firmwareVersionCount.set(count); }
        public void updatePilotActiveDevices(int count) { pilotActiveDevices.set(count); }
        public void updatePilotDataQualityAverage(int avg) { pilotDataQualityAvg.set(avg); }
        public void updatePilotIncidents(int count) { pilotIncidents.set(count); }
    }
}
