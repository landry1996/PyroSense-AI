package com.pyrosense.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "pyrosense.dashboard.cache")
public record DashboardProperties(
        Duration overviewTtl,
        Duration riskyBuildingsTtl,
        Duration riskTrendTtl,
        Duration deviceHealthTtl) {

    public DashboardProperties {
        if (overviewTtl == null) overviewTtl = Duration.ofSeconds(30);
        if (riskyBuildingsTtl == null) riskyBuildingsTtl = Duration.ofSeconds(60);
        if (riskTrendTtl == null) riskTrendTtl = Duration.ofSeconds(300);
        if (deviceHealthTtl == null) deviceHealthTtl = Duration.ofSeconds(60);
    }
}
