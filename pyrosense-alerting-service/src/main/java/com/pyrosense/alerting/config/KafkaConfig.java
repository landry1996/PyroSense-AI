package com.pyrosense.alerting.config;

import com.pyrosense.shared.event.IntegrationEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${pyrosense.alerting.kafka.dlq-topic:pyrosense.dead-letter.events}")
    private String dlqTopic;

    @Value("${pyrosense.alerting.kafka.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${pyrosense.alerting.kafka.retry.backoff-ms:1000}")
    private long backoffMs;

    @Value("${pyrosense.alerting.kafka.retry.backoff-multiplier:2}")
    private double backoffMultiplier;

    @Bean
    public ConsumerFactory<String, IntegrationEvent> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaProperties.getConsumer().getAutoOffsetReset());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.pyrosense.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, IntegrationEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IntegrationEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, IntegrationEvent> consumerFactory,
            KafkaTemplate<String, IntegrationEvent> kafkaTemplate) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, IntegrationEvent>();
        factory.setConsumerFactory(consumerFactory);

        var backoff = new ExponentialBackOff(backoffMs, backoffMultiplier);
        backoff.setMaxElapsedTime(backoffMs * (long) Math.pow(backoffMultiplier, maxRetryAttempts));

        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                        (record, ex) -> new org.apache.kafka.common.TopicPartition(dlqTopic, record.partition())),
                backoff));
        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }

    @Bean
    public ProducerFactory<String, IntegrationEvent> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, IntegrationEvent> kafkaTemplate(ProducerFactory<String, IntegrationEvent> producerFactory) {
        KafkaTemplate<String, IntegrationEvent> template = new KafkaTemplate<>(producerFactory);
        template.setObservationEnabled(true);
        return template;
    }
}
