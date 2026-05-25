package com.pyrosense.ingestion.adapter.out.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pyrosense.ingestion.adapter.out.messaging.KafkaTelemetryEventPublisher;
import com.pyrosense.ingestion.domain.event.TelemetryReceivedEvent;
import com.pyrosense.shared.event.IntegrationEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
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

/**
 * Integration test for KafkaTelemetryEventPublisher using a real Kafka container.
 * Verifies that telemetry events are published to the correct topic with the
 * expected IntegrationEvent envelope structure.
 */
@SpringBootTest(classes = {
        KafkaAutoConfiguration.class,
        KafkaTelemetryEventPublisher.class
})
@Testcontainers
@ActiveProfiles("test")
class KafkaEventPublisherIT {

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaTelemetryEventPublisher publisher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        publisher = new KafkaTelemetryEventPublisher(kafkaTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should publish telemetry event to telemetry-events topic")
    void shouldPublishTelemetryEventToCorrectTopic() throws Exception {
        // Given
        var deviceId = DeviceId.generate();
        var tenantId = TenantId.generate();
        var event = new TelemetryReceivedEvent(
                UUID.randomUUID(),
                Instant.now(),
                deviceId,
                tenantId,
                Instant.now(),
                12.5,   // rmsCurrent
                230.0,  // rmsVoltage
                2875.0, // activePower
                0.95,   // powerFactor
                3.2,    // thd
                42.5,   // temperatureCelsius
                0.02,   // hfNoiseLevel
                0,      // microArcCount
                1       // transientCount
        );

        // When
        publisher.publish(event);

        // Then - consume from Kafka and verify
        try (var consumer = createConsumer()) {
            consumer.subscribe(List.of("telemetry-events"));

            ConsumerRecords<String, String> records = ConsumerRecords.empty();
            int attempts = 0;
            while (records.isEmpty() && attempts < 10) {
                records = consumer.poll(Duration.ofSeconds(1));
                attempts++;
            }

            assertThat(records.count()).isEqualTo(1);

            var record = records.iterator().next();
            assertThat(record.key()).isEqualTo(event.eventId().toString());

            // Verify IntegrationEvent envelope structure
            var integrationEvent = objectMapper.readValue(record.value(), IntegrationEvent.class);
            assertThat(integrationEvent.eventId()).isEqualTo(event.eventId());
            assertThat(integrationEvent.eventType()).isEqualTo("ingestion.telemetry.received");
            assertThat(integrationEvent.source()).isEqualTo("ingestion-service");
            assertThat(integrationEvent.occurredAt()).isNotNull();
            assertThat(integrationEvent.payload()).isNotBlank();
        }
    }

    @Test
    @DisplayName("Should publish to DLQ topic with correct structure")
    void shouldPublishToDlqTopic() throws Exception {
        // Given
        var key = UUID.randomUUID().toString();
        var payload = "{\"invalidData\": true}";
        var reason = "Validation failed: missing required fields";

        // When
        publisher.publishToDlq("telemetry-events", key, payload, reason);

        // Then
        try (var consumer = createConsumer()) {
            consumer.subscribe(List.of("ingestion-dlq"));

            ConsumerRecords<String, String> records = ConsumerRecords.empty();
            int attempts = 0;
            while (records.isEmpty() && attempts < 10) {
                records = consumer.poll(Duration.ofSeconds(1));
                attempts++;
            }

            assertThat(records.count()).isEqualTo(1);

            var record = records.iterator().next();
            assertThat(record.key()).isEqualTo(key);

            // Verify DLQ message contains expected fields
            var dlqMessage = objectMapper.readTree(record.value());
            assertThat(dlqMessage.get("key").asText()).isEqualTo(key);
            assertThat(dlqMessage.get("payload").asText()).isEqualTo(payload);
            assertThat(dlqMessage.get("reason").asText()).isEqualTo(reason);
        }
    }

    @Test
    @DisplayName("Should use event ID as Kafka message key for partitioning")
    void shouldUseEventIdAsMessageKey() throws Exception {
        // Given
        var eventId = UUID.randomUUID();
        var event = new TelemetryReceivedEvent(
                eventId,
                Instant.now(),
                DeviceId.generate(),
                TenantId.generate(),
                Instant.now(),
                10.0, 220.0, 2200.0, 0.9, 2.0, 35.0, 0.01, 0, 0
        );

        // When
        publisher.publish(event);

        // Then
        try (var consumer = createConsumer()) {
            consumer.subscribe(List.of("telemetry-events"));

            ConsumerRecords<String, String> records = ConsumerRecords.empty();
            int attempts = 0;
            while (records.isEmpty() && attempts < 10) {
                records = consumer.poll(Duration.ofSeconds(1));
                attempts++;
            }

            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            var record = records.iterator().next();
            assertThat(record.key()).isEqualTo(eventId.toString());
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()
        ));
    }
}
