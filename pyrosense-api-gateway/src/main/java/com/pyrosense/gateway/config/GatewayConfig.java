package com.pyrosense.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Value("${pyrosense.services.identity-url:http://localhost:8081}")
    private String identityUrl;

    @Value("${pyrosense.services.device-url:http://localhost:8082}")
    private String deviceUrl;

    @Value("${pyrosense.services.ingestion-url:http://localhost:8083}")
    private String ingestionUrl;

    @Value("${pyrosense.services.analysis-url:http://localhost:8084}")
    private String analysisUrl;

    @Value("${pyrosense.services.scoring-url:http://localhost:8085}")
    private String scoringUrl;

    @Value("${pyrosense.services.alerting-url:http://localhost:8086}")
    private String alertingUrl;

    @Value("${pyrosense.services.notification-url:http://localhost:8087}")
    private String notificationUrl;

    @Value("${pyrosense.services.reporting-url:http://localhost:8091}")
    private String reportingUrl;

    @Value("${pyrosense.services.maintenance-url:http://localhost:8089}")
    private String maintenanceUrl;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Identity Service
                .route("identity-users", r -> r.path("/api/v1/users/**")
                        .uri(identityUrl))
                .route("identity-auth", r -> r.path("/api/v1/auth/**")
                        .uri(identityUrl))
                .route("identity-tenants", r -> r.path("/api/v1/tenants/**")
                        .uri(identityUrl))
                .route("identity-audit", r -> r.path("/api/v1/audit-log/**")
                        .uri(identityUrl))

                // Device Service
                .route("devices", r -> r.path("/api/v1/devices/**")
                        .uri(deviceUrl))

                // Ingestion Service
                .route("ingestion", r -> r.path("/api/v1/ingestion/**")
                        .uri(ingestionUrl))

                // Telemetry Query (Ingestion Service)
                .route("telemetry-query", r -> r.path("/api/v1/telemetry/**")
                        .uri(ingestionUrl))

                // Signal Analysis Service
                .route("analysis", r -> r.path("/api/v1/analysis/**")
                        .uri(analysisUrl))

                // Risk Scoring Service
                .route("scoring", r -> r.path("/api/v1/risk/**")
                        .uri(scoringUrl))

                // Alerting Service
                .route("alerting", r -> r.path("/api/v1/alerts/**")
                        .uri(alertingUrl))

                // Notification Service
                .route("notifications", r -> r.path("/api/v1/notifications/**")
                        .uri(notificationUrl))

                // Reporting Service
                .route("reporting", r -> r.path("/api/v1/reports/**")
                        .uri(reportingUrl))

                // Maintenance Service
                .route("maintenance", r -> r.path("/api/v1/interventions/**")
                        .uri(maintenanceUrl))

                .build();
    }
}
