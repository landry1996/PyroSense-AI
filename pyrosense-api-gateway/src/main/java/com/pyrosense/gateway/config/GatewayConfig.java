package com.pyrosense.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("identity", r -> r.path("/api/v1/users/**", "/api/v1/auth/**")
                        .uri("http://localhost:8081"))
                .route("devices", r -> r.path("/api/v1/installations/**", "/api/v1/sensors/**")
                        .uri("http://localhost:8082"))
                .route("ingestion", r -> r.path("/api/v1/signals/**")
                        .uri("http://localhost:8083"))
                .route("analysis", r -> r.path("/api/v1/analysis/**", "/api/v1/baselines/**")
                        .uri("http://localhost:8084"))
                .route("scoring", r -> r.path("/api/v1/risk/**")
                        .uri("http://localhost:8085"))
                .route("alerting", r -> r.path("/api/v1/alerts/**")
                        .uri("http://localhost:8086"))
                .route("notifications", r -> r.path("/api/v1/notifications/**")
                        .uri("http://localhost:8087"))
                .route("reporting", r -> r.path("/api/v1/reports/**")
                        .uri("http://localhost:8088"))
                .route("maintenance", r -> r.path("/api/v1/interventions/**")
                        .uri("http://localhost:8089"))
                .build();
    }
}
