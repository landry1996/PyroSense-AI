package com.pyrosense.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pyrosense.notification.kafka")
public record NotificationKafkaProperties(
        String alertingTopic,
        String scoringTopic,
        String maintenanceTopic,
        String reportingTopic,
        String deviceTopic,
        String dlqTopic,
        String consumerGroupId
) {
    public NotificationKafkaProperties {
        if (alertingTopic == null) alertingTopic = "alerting-events";
        if (scoringTopic == null) scoringTopic = "scoring-events";
        if (maintenanceTopic == null) maintenanceTopic = "maintenance-events";
        if (reportingTopic == null) reportingTopic = "reporting-events";
        if (deviceTopic == null) deviceTopic = "device-events";
        if (dlqTopic == null) dlqTopic = "notification-dlq";
        if (consumerGroupId == null) consumerGroupId = "notification-group";
    }
}
