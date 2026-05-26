package com.pyrosense.dashboard.config;

import com.pyrosense.dashboard.application.port.out.DashboardCachePort;
import com.pyrosense.dashboard.application.port.out.DashboardReadModelPort;
import com.pyrosense.dashboard.application.usecase.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DashboardProperties.class)
public class UseCaseConfig {

    @Bean
    public GetDashboardOverviewService getDashboardOverviewService(
            DashboardReadModelPort readModel, DashboardCachePort cache, DashboardProperties props) {
        return new GetDashboardOverviewService(readModel, cache, props.overviewTtl());
    }

    @Bean
    public GetRiskyBuildingsService getRiskyBuildingsService(
            DashboardReadModelPort readModel, DashboardCachePort cache, DashboardProperties props) {
        return new GetRiskyBuildingsService(readModel, cache, props.riskyBuildingsTtl());
    }

    @Bean
    public GetRiskTrendService getRiskTrendService(
            DashboardReadModelPort readModel, DashboardCachePort cache, DashboardProperties props) {
        return new GetRiskTrendService(readModel, cache, props.riskTrendTtl());
    }

    @Bean
    public GetRecentAlertsService getRecentAlertsService(DashboardReadModelPort readModel) {
        return new GetRecentAlertsService(readModel);
    }

    @Bean
    public GetPriorityInterventionsService getPriorityInterventionsService(DashboardReadModelPort readModel) {
        return new GetPriorityInterventionsService(readModel);
    }

    @Bean
    public GetDeviceHealthService getDeviceHealthService(
            DashboardReadModelPort readModel, DashboardCachePort cache, DashboardProperties props) {
        return new GetDeviceHealthService(readModel, cache, props.deviceHealthTtl());
    }
}
