package com.pyrosense.alerting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "pyrosense.alerting")
public record AlertingProperties(
        Sla sla,
        Kafka kafka
) {
    public AlertingProperties {
        if (sla == null) sla = new Sla(Duration.ofHours(24), Duration.ofDays(7), Duration.ofDays(30), Duration.ofHours(4));
        if (kafka == null) kafka = new Kafka("scoring-events", "analysis-events", "alerting-events");
    }

    public record Sla(
            Duration criticalDeadline,
            Duration warningDeadline,
            Duration infoDeadline,
            Duration escalationInterval
    ) {}

    public record Kafka(
            String scoringTopic,
            String analysisTopic,
            String outputTopic
    ) {}
}
