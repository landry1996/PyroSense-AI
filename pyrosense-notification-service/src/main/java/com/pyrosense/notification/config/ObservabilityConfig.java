package com.pyrosense.notification.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public NotificationMetrics notificationMetrics(MeterRegistry registry) {
        return new NotificationMetrics(registry);
    }

    public static class NotificationMetrics {

        private final Counter notificationsSent;
        private final Counter notificationsFailed;
        private final Counter notificationsSuppressed;
        private final Timer notificationDeliveryDuration;
        private final Counter eventsProcessed;
        private final Counter dlqEvents;
        private final Counter accessDenied;
        private final Counter suspiciousTenantAccess;

        public NotificationMetrics(MeterRegistry registry) {
            this.notificationsSent = Counter.builder("pyrosense.notification.sent")
                    .description("Total notifications sent successfully")
                    .register(registry);

            this.notificationsFailed = Counter.builder("pyrosense.notification.failed")
                    .description("Total notification delivery failures")
                    .register(registry);

            this.notificationsSuppressed = Counter.builder("pyrosense.notification.suppressed")
                    .description("Notifications suppressed (dedup, quiet hours, consent)")
                    .register(registry);

            this.notificationDeliveryDuration = Timer.builder("pyrosense.notification.delivery.duration")
                    .description("Notification delivery duration")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(registry);

            this.eventsProcessed = Counter.builder("pyrosense.notification.events.processed")
                    .description("Kafka events processed by notification service")
                    .register(registry);

            this.dlqEvents = Counter.builder("pyrosense.notification.dlq.events")
                    .description("Events sent to dead letter queue")
                    .register(registry);

            this.accessDenied = Counter.builder("pyrosense.notification.access.denied")
                    .description("Access denied attempts")
                    .register(registry);

            this.suspiciousTenantAccess = Counter.builder("pyrosense.notification.suspicious.tenant.access")
                    .description("Suspicious cross-tenant access attempts")
                    .register(registry);
        }

        public Counter notificationsSent() {
            return notificationsSent;
        }

        public Counter notificationsFailed() {
            return notificationsFailed;
        }

        public Counter notificationsSuppressed() {
            return notificationsSuppressed;
        }

        public Timer notificationDeliveryDuration() {
            return notificationDeliveryDuration;
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

        public Counter suspiciousTenantAccess() {
            return suspiciousTenantAccess;
        }
    }
}
