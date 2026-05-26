package com.pyrosense.alerting.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pyrosense.alerting.domain.event.AlertCreatedEvent;
import com.pyrosense.alerting.domain.model.AlertType;
import com.pyrosense.shared.event.IntegrationEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.valueobject.AlertSeverity;
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
        com.pyrosense.alerting.config.KafkaConfig.class
})
@Testcontainers
class KafkaAlertEventPublisherIT {

    private static final String TOPIC = "pyrosense.alerts.events";

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "alerting-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("pyrosense.alerting.kafka.output-topic", () -> TOPIC);
        registry.add("pyrosense.alerting.kafka.dlq-topic", () -> "pyrosense.dead-letter.events");
    }

    @Autowired
    private KafkaTemplate<String, IntegrationEvent> kafkaTemplate;

    private KafkaAlertEventPublisher publisher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        publisher = new KafkaAlertEventPublisher(kafkaTemplate, objectMapper, TOPIC);
    }

    @Test
    void shouldPublishAlertCreatedEventWithEnrichedEnvelope() throws Exception {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        AlertId alertId = new AlertId(UUID.randomUUID());
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        var event = new AlertCreatedEvent(
                UUID.randomUUID(), Instant.now(),
                alertId, tenantId, deviceId,
                AlertSeverity.CRITICAL, AlertType.OVERHEATING
        );

        publisher.publish(event, "corr-123", tenantId.value().toString());

        try (var consumer = createConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecords<String, IntegrationEvent> records =
                    consumer.poll(Duration.ofSeconds(10));

            assertThat(records).isNotEmpty();
            IntegrationEvent received = records.iterator().next().value();

            assertThat(received.eventId()).isEqualTo(event.eventId());
            assertThat(received.eventType()).isEqualTo("alerting.alert.created");
            assertThat(received.version()).isEqualTo(1);
            assertThat(received.sourceService()).isEqualTo("pyrosense-alerting-service");
            assertThat(received.tenantId()).isEqualTo(tenantId.value().toString());
            assertThat(received.correlationId()).isEqualTo("corr-123");
            assertThat(received.causationId()).isEqualTo(event.eventId().toString());
            assertThat(received.payload()).contains(alertId.value().toString());
        }
    }

    @Test
    void shouldUseEventIdAsKafkaMessageKey() throws Exception {
        var event = new AlertCreatedEvent(
                UUID.randomUUID(), Instant.now(),
                new AlertId(UUID.randomUUID()),
                new TenantId(UUID.randomUUID()),
                new DeviceId(UUID.randomUUID()),
                AlertSeverity.WARNING, AlertType.INSULATION_DEGRADATION
        );

        publisher.publish(event);

        try (var consumer = createConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecords<String, IntegrationEvent> records =
                    consumer.poll(Duration.ofSeconds(10));

            assertThat(records).isNotEmpty();
            String key = records.iterator().next().key();
            assertThat(key).isEqualTo(event.eventId().toString());
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
