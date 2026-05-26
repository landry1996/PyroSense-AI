package com.pyrosense.maintenance.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pyrosense.maintenance.domain.event.MaintenanceInterventionCreatedEvent;
import com.pyrosense.maintenance.domain.model.InterventionPriority;
import com.pyrosense.maintenance.domain.model.InterventionType;
import com.pyrosense.shared.event.IntegrationEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        KafkaAutoConfiguration.class,
        com.pyrosense.maintenance.config.KafkaConfig.class
})
@Testcontainers
class KafkaMaintenanceEventPublisherIT {

    private static final String TOPIC = "pyrosense.maintenance.events";

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "maintenance-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("pyrosense.maintenance.kafka.output-topic", () -> TOPIC);
        registry.add("pyrosense.maintenance.kafka.dlq-topic", () -> "pyrosense.dead-letter.events");
    }

    @Autowired
    private KafkaTemplate<String, IntegrationEvent> kafkaTemplate;

    private KafkaMaintenanceEventPublisher publisher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        publisher = new KafkaMaintenanceEventPublisher(kafkaTemplate, objectMapper, TOPIC);
    }

    @Test
    void shouldPublishInterventionCreatedWithCorrelationMetadata() throws Exception {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        UUID interventionId = UUID.randomUUID();
        AlertId alertId = new AlertId(UUID.randomUUID());

        var event = new MaintenanceInterventionCreatedEvent(
                UUID.randomUUID(), Instant.now(),
                interventionId, tenantId, alertId,
                new DeviceId(UUID.randomUUID()),
                InterventionType.CORRECTIVE, InterventionPriority.URGENT
        );

        publisher.publish(event, "corr-456", tenantId.value().toString());

        try (var consumer = createConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecords<String, IntegrationEvent> records =
                    consumer.poll(Duration.ofSeconds(10));

            assertThat(records).isNotEmpty();
            IntegrationEvent received = records.iterator().next().value();

            assertThat(received.eventType()).isEqualTo("maintenance.intervention.created");
            assertThat(received.version()).isEqualTo(1);
            assertThat(received.sourceService()).isEqualTo("pyrosense-maintenance-service");
            assertThat(received.tenantId()).isEqualTo(tenantId.value().toString());
            assertThat(received.correlationId()).isEqualTo("corr-456");
            assertThat(received.payload()).contains(interventionId.toString());
        }
    }

    @Test
    void shouldDefaultCorrelationIdWhenNotProvided() throws Exception {
        var event = new MaintenanceInterventionCreatedEvent(
                UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), new TenantId(UUID.randomUUID()),
                new AlertId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()),
                InterventionType.PREVENTIVE, InterventionPriority.MEDIUM
        );

        publisher.publish(event);

        try (var consumer = createConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecords<String, IntegrationEvent> records =
                    consumer.poll(Duration.ofSeconds(10));

            assertThat(records).isNotEmpty();
            IntegrationEvent received = records.iterator().next().value();
            assertThat(received.correlationId()).isEqualTo(received.eventId().toString());
        }
    }

    private KafkaConsumer<String, IntegrationEvent> createConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                JsonDeserializer.TRUSTED_PACKAGES, "com.pyrosense.*",
                JsonDeserializer.VALUE_DEFAULT_TYPE, IntegrationEvent.class.getName()
        ));
    }
}
