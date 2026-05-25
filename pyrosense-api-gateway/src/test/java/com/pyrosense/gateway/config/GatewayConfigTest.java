package com.pyrosense.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayConfigTest {

    @Test
    void shouldHaveAllServiceUrlFields() {
        GatewayConfig config = new GatewayConfig();
        ReflectionTestUtils.setField(config, "identityUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(config, "deviceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(config, "ingestionUrl", "http://localhost:8083");
        ReflectionTestUtils.setField(config, "analysisUrl", "http://localhost:8084");
        ReflectionTestUtils.setField(config, "scoringUrl", "http://localhost:8085");
        ReflectionTestUtils.setField(config, "alertingUrl", "http://localhost:8086");
        ReflectionTestUtils.setField(config, "notificationUrl", "http://localhost:8087");
        ReflectionTestUtils.setField(config, "reportingUrl", "http://localhost:8091");
        ReflectionTestUtils.setField(config, "maintenanceUrl", "http://localhost:8089");

        assertThat(ReflectionTestUtils.getField(config, "identityUrl")).isEqualTo("http://localhost:8081");
        assertThat(ReflectionTestUtils.getField(config, "deviceUrl")).isEqualTo("http://localhost:8082");
        assertThat(ReflectionTestUtils.getField(config, "ingestionUrl")).isEqualTo("http://localhost:8083");
        assertThat(ReflectionTestUtils.getField(config, "analysisUrl")).isEqualTo("http://localhost:8084");
        assertThat(ReflectionTestUtils.getField(config, "scoringUrl")).isEqualTo("http://localhost:8085");
        assertThat(ReflectionTestUtils.getField(config, "alertingUrl")).isEqualTo("http://localhost:8086");
        assertThat(ReflectionTestUtils.getField(config, "notificationUrl")).isEqualTo("http://localhost:8087");
        assertThat(ReflectionTestUtils.getField(config, "reportingUrl")).isEqualTo("http://localhost:8091");
        assertThat(ReflectionTestUtils.getField(config, "maintenanceUrl")).isEqualTo("http://localhost:8089");
    }

    @Test
    void shouldMapToCorrectPorts() {
        GatewayConfig config = new GatewayConfig();
        ReflectionTestUtils.setField(config, "identityUrl", "http://identity:8081");
        ReflectionTestUtils.setField(config, "deviceUrl", "http://devices:8082");
        ReflectionTestUtils.setField(config, "ingestionUrl", "http://ingestion:8083");
        ReflectionTestUtils.setField(config, "analysisUrl", "http://analysis:8084");
        ReflectionTestUtils.setField(config, "scoringUrl", "http://scoring:8085");
        ReflectionTestUtils.setField(config, "alertingUrl", "http://alerting:8086");
        ReflectionTestUtils.setField(config, "notificationUrl", "http://notification:8087");
        ReflectionTestUtils.setField(config, "reportingUrl", "http://reporting:8091");
        ReflectionTestUtils.setField(config, "maintenanceUrl", "http://maintenance:8089");

        assertThat(ReflectionTestUtils.getField(config, "reportingUrl")).isEqualTo("http://reporting:8091");
        assertThat(ReflectionTestUtils.getField(config, "maintenanceUrl")).isEqualTo("http://maintenance:8089");
    }
}
