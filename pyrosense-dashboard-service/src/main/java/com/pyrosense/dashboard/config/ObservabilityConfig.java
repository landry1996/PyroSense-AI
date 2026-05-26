package com.pyrosense.dashboard.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public DashboardMetrics dashboardMetrics(MeterRegistry registry) {
        return new DashboardMetrics(registry);
    }

    public static class DashboardMetrics {

        private final Counter overviewRequests;
        private final Timer overviewLatency;
        private final Counter cacheHits;
        private final Counter cacheMisses;
        private final Counter accessDenied;
        private final Counter suspiciousTenantAccess;
        private final Counter eventsProcessed;

        public DashboardMetrics(MeterRegistry registry) {
            this.overviewRequests = Counter.builder("pyrosense.dashboard.overview.requests")
                    .description("Total dashboard overview requests")
                    .register(registry);

            this.overviewLatency = Timer.builder("pyrosense.dashboard.overview.latency")
                    .description("Dashboard overview request latency")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(registry);

            this.cacheHits = Counter.builder("pyrosense.dashboard.cache.hits")
                    .description("Dashboard cache hits")
                    .register(registry);

            this.cacheMisses = Counter.builder("pyrosense.dashboard.cache.misses")
                    .description("Dashboard cache misses")
                    .register(registry);

            this.accessDenied = Counter.builder("pyrosense.dashboard.access.denied")
                    .description("Access denied attempts")
                    .register(registry);

            this.suspiciousTenantAccess = Counter.builder("pyrosense.dashboard.suspicious.tenant.access")
                    .description("Suspicious cross-tenant access attempts")
                    .register(registry);

            this.eventsProcessed = Counter.builder("pyrosense.dashboard.events.processed")
                    .description("Kafka cache invalidation events processed")
                    .register(registry);
        }

        public Counter overviewRequests() {
            return overviewRequests;
        }

        public Timer overviewLatency() {
            return overviewLatency;
        }

        public Counter cacheHits() {
            return cacheHits;
        }

        public Counter cacheMisses() {
            return cacheMisses;
        }

        public Counter accessDenied() {
            return accessDenied;
        }

        public Counter suspiciousTenantAccess() {
            return suspiciousTenantAccess;
        }

        public Counter eventsProcessed() {
            return eventsProcessed;
        }
    }
}
