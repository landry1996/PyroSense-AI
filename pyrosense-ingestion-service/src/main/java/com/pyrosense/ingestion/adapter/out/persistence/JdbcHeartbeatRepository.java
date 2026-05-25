package com.pyrosense.ingestion.adapter.out.persistence;

import com.pyrosense.ingestion.application.port.out.HeartbeatRepositoryPort;
import com.pyrosense.ingestion.domain.model.DeviceHeartbeat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class JdbcHeartbeatRepository implements HeartbeatRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcHeartbeatRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(DeviceHeartbeat heartbeat) {
        jdbcTemplate.update("""
                INSERT INTO device_heartbeats (tenant_id, device_id, received_at,
                    firmware_version, uptime_hours, memory_usage_percent, cpu_temperature_celsius)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                heartbeat.tenantId().value(),
                heartbeat.deviceId().value(),
                Timestamp.from(heartbeat.timestamp()),
                heartbeat.firmwareVersion(),
                heartbeat.uptimeHours(),
                heartbeat.memoryUsagePercent(),
                heartbeat.cpuTemperatureCelsius());
    }
}
