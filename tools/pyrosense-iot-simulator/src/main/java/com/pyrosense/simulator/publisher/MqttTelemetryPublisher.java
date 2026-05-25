package com.pyrosense.simulator.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.pyrosense.simulator.config.SimulatorConfig;
import com.pyrosense.simulator.domain.TelemetryReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class MqttTelemetryPublisher implements TelemetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryPublisher.class);

    private final SimulatorConfig config;
    private final ObjectMapper objectMapper;
    private Mqtt5BlockingClient client;

    public MqttTelemetryPublisher(SimulatorConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void connect() {
        try {
            client = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost(config.mqttHost())
                    .serverPort(config.mqttPort())
                    .identifier("pyrosense-simulator-" + System.currentTimeMillis())
                    .buildBlocking();
            client.connect();
            log.info("Connected to MQTT broker at {}:{}", config.mqttHost(), config.mqttPort());
        } catch (Exception e) {
            log.error("Failed to connect to MQTT broker: {}", e.getMessage());
            throw new RuntimeException("MQTT connection failed", e);
        }
    }

    @Override
    public void publish(TelemetryReading reading) {
        try {
            String topic = "pyrosense/" + reading.tenantId() + "/" + reading.deviceId() + "/telemetry";
            String payload = objectMapper.writeValueAsString(reading);
            client.publishWith()
                    .topic(topic)
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .send();
            log.debug("Published to {}", topic);
        } catch (Exception e) {
            log.warn("MQTT publish failed: {}", e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        if (client != null) {
            try {
                client.disconnect();
                log.info("Disconnected from MQTT broker");
            } catch (Exception e) {
                log.warn("Error disconnecting MQTT: {}", e.getMessage());
            }
        }
    }

    @Override
    public String name() {
        return "MQTT";
    }
}
