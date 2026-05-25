package com.pyrosense.ingestion.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic telemetryEventsTopic() {
        return TopicBuilder.name("telemetry-events")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ingestionDlqTopic() {
        return TopicBuilder.name("ingestion-dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
