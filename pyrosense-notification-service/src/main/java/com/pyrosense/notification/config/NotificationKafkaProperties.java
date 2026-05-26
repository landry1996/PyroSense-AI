package com.pyrosense.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pyrosense.notification.kafka")
public record NotificationKafkaProperties(
        String alertingTopic,
        String scoringTopic,
        String maintenanceTopic,
        String reportingTopic,
        String deviceTopic,
        String outputTopic,
        String dlqTopic,
        String consumerGroupId
) {
    public NotificationKafkaProperties {
        if (alertingTopic == null) alertingTopic = "pyrosense.alerts.events";
        if (scoringTopic == null) scoringTopic = "pyrosense.scoring.events";
        if (maintenanceTopic == null) maintenanceTopic = "pyrosense.maintenance.events";
        if (reportingTopic == null) reportingTopic = "pyrosense.reports.events";
        if (deviceTopic == null) deviceTopic = "pyrosense.device.events";
        if (outputTopic == null) outputTopic = "pyrosense.notifications.events";
        if (dlqTopic == null) dlqTopic = "pyrosense.dead-letter.events";
        if (consumerGroupId == null) consumerGroupId = "notification-group";
    }
}
