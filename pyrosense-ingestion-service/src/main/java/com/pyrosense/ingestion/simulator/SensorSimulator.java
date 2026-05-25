package com.pyrosense.ingestion.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SensorSimulator {

    private static final Random RANDOM = new Random();
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final String tenantId;
    private final String deviceId;
    private final String brokerHost;
    private final int brokerPort;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SensorSimulator(String tenantId, String deviceId, String brokerHost, int brokerPort) {
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
    }

    public void start(int intervalSeconds) {
        Mqtt5BlockingClient client = MqttClient.builder()
                .useMqttVersion5()
                .identifier("simulator-" + deviceId)
                .serverHost(brokerHost)
                .serverPort(brokerPort)
                .buildBlocking();

        client.connect();
        System.out.println("Simulator connected for device: " + deviceId);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                publishTelemetry(client);
                if (RANDOM.nextInt(10) == 0) {
                    publishHeartbeat(client);
                }
            } catch (Exception e) {
                System.err.println("Simulation error: " + e.getMessage());
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    private void publishTelemetry(Mqtt5BlockingClient client) throws Exception {
        var payload = new TelemetryMessage(
                deviceId,
                Instant.now().toString(),
                1000,
                10.0 + RANDOM.nextDouble() * 5.0,
                228.0 + RANDOM.nextDouble() * 5.0,
                2200.0 + RANDOM.nextDouble() * 800.0,
                200.0 + RANDOM.nextDouble() * 200.0,
                0.85 + RANDOM.nextDouble() * 0.14,
                2.0 + RANDOM.nextDouble() * 8.0,
                25.0 + RANDOM.nextDouble() * 20.0,
                RANDOM.nextDouble() * 0.5,
                RANDOM.nextInt(5) == 0 ? RANDOM.nextInt(3) + 1 : 0,
                RANDOM.nextInt(8) == 0 ? 1 : 0,
                "1.0.0"
        );

        String json = MAPPER.writeValueAsString(payload);
        String topic = "pyrosense/" + tenantId + "/" + deviceId + "/telemetry";
        client.publishWith().topic(topic).payload(json.getBytes(StandardCharsets.UTF_8)).send();
    }

    private void publishHeartbeat(Mqtt5BlockingClient client) throws Exception {
        var payload = new HeartbeatMessage(
                deviceId,
                Instant.now().toString(),
                "1.0.0",
                RANDOM.nextDouble() * 720.0,
                30.0 + RANDOM.nextDouble() * 40.0,
                35.0 + RANDOM.nextDouble() * 15.0
        );

        String json = MAPPER.writeValueAsString(payload);
        String topic = "pyrosense/" + tenantId + "/" + deviceId + "/heartbeat";
        client.publishWith().topic(topic).payload(json.getBytes(StandardCharsets.UTF_8)).send();
    }

    public void stop() {
        scheduler.shutdown();
    }

    public static void main(String[] args) {
        String tenantId = args.length > 0 ? args[0] : UUID.randomUUID().toString();
        String deviceId = args.length > 1 ? args[1] : UUID.randomUUID().toString();
        String host = args.length > 2 ? args[2] : "localhost";
        int port = args.length > 3 ? Integer.parseInt(args[3]) : 1883;
        int intervalSec = args.length > 4 ? Integer.parseInt(args[4]) : 5;

        System.out.println("Starting simulator: tenant=" + tenantId + " device=" + deviceId);
        var simulator = new SensorSimulator(tenantId, deviceId, host, port);
        simulator.start(intervalSec);

        Runtime.getRuntime().addShutdownHook(new Thread(simulator::stop));
    }

    record TelemetryMessage(String deviceId, String timestamp, int samplingWindowMs,
                            double rmsCurrent, double rmsVoltage, double activePower,
                            double reactivePower, double powerFactor, double thd,
                            double temperatureCelsius, double hfNoiseLevel,
                            int microArcCount, int transientCount, String firmwareVersion) {}

    record HeartbeatMessage(String deviceId, String timestamp, String firmwareVersion,
                            double uptimeHours, double memoryUsagePercent, double cpuTemperatureCelsius) {}
}
