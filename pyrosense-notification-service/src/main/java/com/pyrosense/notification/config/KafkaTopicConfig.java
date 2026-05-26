package com.pyrosense.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private final NotificationKafkaProperties properties;

    public KafkaTopicConfig(NotificationKafkaProperties properties) {
        this.properties = properties;
    }

    @Bean
    public NewTopic alertingTopic() {
        return TopicBuilder.name(properties.alertingTopic())
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic scoringTopic() {
        return TopicBuilder.name(properties.scoringTopic())
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic maintenanceTopic() {
        return TopicBuilder.name(properties.maintenanceTopic())
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reportingTopic() {
        return TopicBuilder.name(properties.reportingTopic())
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic deviceTopic() {
        return TopicBuilder.name(properties.deviceTopic())
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationOutputTopic() {
        return TopicBuilder.name(properties.outputTopic())
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(properties.dlqTopic())
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }
}
