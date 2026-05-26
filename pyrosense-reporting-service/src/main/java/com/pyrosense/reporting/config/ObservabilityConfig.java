package com.pyrosense.reporting.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public ReportingMetrics reportingMetrics(MeterRegistry registry) {
        return new ReportingMetrics(registry);
    }

    public static class ReportingMetrics {

        private final Counter reportsRequested;
        private final Counter reportsGenerated;
        private final Counter reportsFailed;
        private final Timer reportGenerationDuration;
        private final Counter reportDownloads;
        private final Counter reportDownloadDenied;
        private final Counter eventsProcessed;
        private final Counter dlqEvents;
        private final Counter accessDenied;

        public ReportingMetrics(MeterRegistry registry) {
            this.reportsRequested = Counter.builder("pyrosense.reporting.reports.requested")
                    .description("Total report generation requests")
                    .register(registry);

            this.reportsGenerated = Counter.builder("pyrosense.reporting.reports.generated")
                    .description("Total reports successfully generated")
                    .register(registry);

            this.reportsFailed = Counter.builder("pyrosense.reporting.reports.failed")
                    .description("Total report generation failures")
                    .register(registry);

            this.reportGenerationDuration = Timer.builder("pyrosense.reporting.generation.duration")
                    .description("Report generation duration")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(registry);

            this.reportDownloads = Counter.builder("pyrosense.reporting.downloads")
                    .description("Total report downloads")
                    .register(registry);

            this.reportDownloadDenied = Counter.builder("pyrosense.reporting.downloads.denied")
                    .description("Report download attempts denied (expired/invalid token)")
                    .register(registry);

            this.eventsProcessed = Counter.builder("pyrosense.reporting.events.processed")
                    .description("Kafka events processed")
                    .register(registry);

            this.dlqEvents = Counter.builder("pyrosense.reporting.dlq.events")
                    .description("Events sent to dead letter queue")
                    .register(registry);

            this.accessDenied = Counter.builder("pyrosense.reporting.access.denied")
                    .description("Access denied attempts")
                    .register(registry);
        }

        public Counter reportsRequested() {
            return reportsRequested;
        }

        public Counter reportsGenerated() {
            return reportsGenerated;
        }

        public Counter reportsFailed() {
            return reportsFailed;
        }

        public Timer reportGenerationDuration() {
            return reportGenerationDuration;
        }

        public Counter reportDownloads() {
            return reportDownloads;
        }

        public Counter reportDownloadDenied() {
            return reportDownloadDenied;
        }

        public Counter eventsProcessed() {
            return eventsProcessed;
        }

        public Counter dlqEvents() {
            return dlqEvents;
        }

        public Counter accessDenied() {
            return accessDenied;
        }
    }
}
