package com.pyrosense.simulator.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pyrosense.simulator.config.SimulatorConfig;
import com.pyrosense.simulator.domain.TelemetryReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class RestTelemetryPublisher implements TelemetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(RestTelemetryPublisher.class);

    private final SimulatorConfig config;
    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    public RestTelemetryPublisher(SimulatorConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void connect() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        log.info("REST publisher ready, target: {}", config.restBaseUrl());
    }

    @Override
    public void publish(TelemetryReading reading) {
        try {
            String json = objectMapper.writeValueAsString(reading);
            String url = config.restBaseUrl() + "/api/v1/ingestion/telemetry";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("X-Tenant-Id", reading.tenantId())
                    .header("X-Device-Id", reading.deviceId())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("REST publish failed: HTTP {} - {}", response.statusCode(), response.body());
            } else {
                log.debug("REST publish OK: HTTP {}", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("REST publish error: {}", e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        log.info("REST publisher disconnected");
    }

    @Override
    public String name() {
        return "REST";
    }
}
