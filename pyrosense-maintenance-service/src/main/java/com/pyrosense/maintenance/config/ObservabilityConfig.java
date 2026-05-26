package com.pyrosense.maintenance.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class ObservabilityConfig {

    @Bean
    public MaintenanceMetrics maintenanceMetrics(MeterRegistry registry) {
        return new MaintenanceMetrics(registry);
    }

    public static class MaintenanceMetrics {

        private final Counter interventionsCreated;
        private final Counter interventionsCompleted;
        private final AtomicLong interventionsOverdue;
        private final Counter eventsProcessed;
        private final Counter dlqEvents;
        private final Counter accessDenied;
        private final Counter suspiciousTenantAccess;

        public MaintenanceMetrics(MeterRegistry registry) {
            this.interventionsCreated = Counter.builder("pyrosense.maintenance.interventions.created")
                    .description("Total interventions created")
                    .register(registry);

            this.interventionsCompleted = Counter.builder("pyrosense.maintenance.interventions.completed")
                    .description("Total interventions completed")
                    .register(registry);

            this.interventionsOverdue = new AtomicLong(0);
            Gauge.builder("pyrosense.maintenance.interventions.overdue", interventionsOverdue, AtomicLong::get)
                    .description("Current number of overdue interventions")
                    .register(registry);

            this.eventsProcessed = Counter.builder("pyrosense.maintenance.events.processed")
                    .description("Kafka events processed by maintenance service")
                    .register(registry);

            this.dlqEvents = Counter.builder("pyrosense.maintenance.dlq.events")
                    .description("Events sent to dead letter queue")
                    .register(registry);

            this.accessDenied = Counter.builder("pyrosense.maintenance.access.denied")
                    .description("Access denied attempts")
                    .register(registry);

            this.suspiciousTenantAccess = Counter.builder("pyrosense.maintenance.suspicious.tenant.access")
                    .description("Suspicious cross-tenant access attempts")
                    .register(registry);

            Counter.builder("pyrosense.maintenance.interventions.by.status")
                    .tag("status", "CREATED")
                    .description("Interventions by status")
                    .register(registry);
        }

        public Counter interventionsCreated() {
            return interventionsCreated;
        }

        public Counter interventionsCompleted() {
            return interventionsCompleted;
        }

        public void setOverdueCount(long count) {
            interventionsOverdue.set(count);
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
