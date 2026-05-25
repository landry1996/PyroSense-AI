package com.pyrosense.shared.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Kafka container for integration tests.
 * Uses KRaft mode (no ZooKeeper) with Apache Kafka 3.8.0.
 */
public class KafkaContainerConfig {

    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    static {
        KAFKA.start();
    }

    private KafkaContainerConfig() {}

    /**
     * Registers Kafka bootstrap servers from the running container.
     * Call this from @DynamicPropertySource in test classes.
     */
    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    public static String getBootstrapServers() {
        return KAFKA.getBootstrapServers();
    }
}
