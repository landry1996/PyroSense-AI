package com.pyrosense.dashboard.adapter.out.persistence;

import com.pyrosense.dashboard.application.port.out.PilotRepositoryPort;
import com.pyrosense.dashboard.domain.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPilotRepository implements PilotRepositoryPort {

    private final JdbcTemplate jdbc;

    public JdbcPilotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void savePilot(PilotProgram pilot) {
        jdbc.update("""
                INSERT INTO pilot_programs (id, tenant_id, name, description, status, created_at, started_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                pilot.getId(), pilot.getTenantId(), pilot.getName(), pilot.getDescription(),
                pilot.getStatus().name(), Timestamp.from(pilot.getCreatedAt()),
                pilot.getStartedAt() != null ? Timestamp.from(pilot.getStartedAt()) : null,
                pilot.getCompletedAt() != null ? Timestamp.from(pilot.getCompletedAt()) : null);
    }

    @Override
    public void updatePilotStatus(PilotProgram pilot) {
        jdbc.update("""
                UPDATE pilot_programs SET status = ?, started_at = ?, completed_at = ? WHERE id = ?
                """,
                pilot.getStatus().name(),
                pilot.getStartedAt() != null ? Timestamp.from(pilot.getStartedAt()) : null,
                pilot.getCompletedAt() != null ? Timestamp.from(pilot.getCompletedAt()) : null,
                pilot.getId());
    }

    @Override
    public Optional<PilotProgram> findPilotById(UUID id) {
        var results = jdbc.query("SELECT * FROM pilot_programs WHERE id = ?", this::mapPilot, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<PilotProgram> findPilotsByTenant(String tenantId) {
        return jdbc.query("SELECT * FROM pilot_programs WHERE tenant_id = ? ORDER BY created_at DESC",
                this::mapPilot, tenantId);
    }

    @Override
    public void saveDevice(PilotDevice device) {
        jdbc.update("""
                INSERT INTO pilot_devices (id, pilot_id, device_id, serial_number, site_name,
                    circuit_description, status, installed_at, removed_at, installation_notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                device.id(), device.pilotId(), device.deviceId(), device.serialNumber(),
                device.siteName(), device.circuitDescription(), device.status().name(),
                device.installedAt() != null ? Timestamp.from(device.installedAt()) : null,
                device.removedAt() != null ? Timestamp.from(device.removedAt()) : null,
                device.installationNotes());
    }

    @Override
    public List<PilotDevice> findDevicesByPilot(UUID pilotId) {
        return jdbc.query("SELECT * FROM pilot_devices WHERE pilot_id = ? ORDER BY serial_number",
                this::mapDevice, pilotId);
    }

    @Override
    public void saveObservation(PilotObservation observation) {
        jdbc.update("""
                INSERT INTO pilot_observations (id, pilot_id, author_id, author_name, type,
                    content, device_id, site_name, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                observation.id(), observation.pilotId(), observation.authorId(),
                observation.authorName(), observation.type().name(), observation.content(),
                observation.deviceId(), observation.siteName(),
                Timestamp.from(observation.createdAt()));
    }

    @Override
    public List<PilotObservation> findObservationsByPilot(UUID pilotId) {
        return jdbc.query("SELECT * FROM pilot_observations WHERE pilot_id = ? ORDER BY created_at DESC",
                this::mapObservation, pilotId);
    }

    @Override
    public void saveIncident(PilotIncident incident) {
        jdbc.update("""
                INSERT INTO pilot_incidents (id, pilot_id, reported_by, severity, category,
                    title, description, device_id, site_name, resolution, status, reported_at, resolved_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                incident.id(), incident.pilotId(), incident.reportedBy(),
                incident.severity().name(), incident.category().name(),
                incident.title(), incident.description(), incident.deviceId(),
                incident.siteName(), incident.resolution(), incident.status().name(),
                Timestamp.from(incident.reportedAt()),
                incident.resolvedAt() != null ? Timestamp.from(incident.resolvedAt()) : null);
    }

    @Override
    public List<PilotIncident> findIncidentsByPilot(UUID pilotId) {
        return jdbc.query("SELECT * FROM pilot_incidents WHERE pilot_id = ? ORDER BY reported_at DESC",
                this::mapIncident, pilotId);
    }

    @Override
    public void saveKpiSnapshot(PilotKpiSnapshot snapshot) {
        jdbc.update("""
                INSERT INTO pilot_kpi_snapshots (id, pilot_id, date, total_devices, active_devices,
                    offline_devices, uptime_percent, telemetry_valid_percent, avg_signal_quality,
                    alerts_generated, alerts_confirmed, false_positives, false_positive_rate,
                    incidents_open, incidents_resolved, avg_latency_ms, computed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                snapshot.id(), snapshot.pilotId(), snapshot.date(),
                snapshot.totalDevices(), snapshot.activeDevices(), snapshot.offlineDevices(),
                snapshot.uptimePercent(), snapshot.telemetryValidPercent(), snapshot.avgSignalQuality(),
                snapshot.alertsGenerated(), snapshot.alertsConfirmed(), snapshot.falsePositives(),
                snapshot.falsePositiveRate(), snapshot.incidentsOpen(), snapshot.incidentsResolved(),
                snapshot.avgLatencyMs(), Timestamp.from(snapshot.computedAt()));
    }

    @Override
    public List<PilotKpiSnapshot> findKpisByPilot(UUID pilotId) {
        return jdbc.query("SELECT * FROM pilot_kpi_snapshots WHERE pilot_id = ? ORDER BY date DESC",
                this::mapKpi, pilotId);
    }

    @Override
    public void saveSite(PilotSite site) {
        jdbc.update("""
                INSERT INTO pilot_sites (id, pilot_id, name, address, contact_name, contact_phone, notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                site.id(), site.pilotId(), site.name(), site.address(),
                site.contactName(), site.contactPhone(), site.notes(),
                Timestamp.from(site.createdAt()));
    }

    @Override
    public List<PilotSite> findSitesByPilot(UUID pilotId) {
        return jdbc.query("SELECT * FROM pilot_sites WHERE pilot_id = ? ORDER BY name",
                this::mapSite, pilotId);
    }

    private PilotProgram mapPilot(ResultSet rs, int rowNum) throws SQLException {
        return new PilotProgram(
                UUID.fromString(rs.getString("id")),
                rs.getString("tenant_id"),
                rs.getString("name"),
                rs.getString("description"),
                PilotStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toInstant() : null,
                rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null);
    }

    private PilotDevice mapDevice(ResultSet rs, int rowNum) throws SQLException {
        return new PilotDevice(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("pilot_id")),
                rs.getString("device_id"),
                rs.getString("serial_number"),
                rs.getString("site_name"),
                rs.getString("circuit_description"),
                PilotDevice.PilotDeviceStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("installed_at") != null ? rs.getTimestamp("installed_at").toInstant() : null,
                rs.getTimestamp("removed_at") != null ? rs.getTimestamp("removed_at").toInstant() : null,
                rs.getString("installation_notes"));
    }

    private PilotObservation mapObservation(ResultSet rs, int rowNum) throws SQLException {
        return new PilotObservation(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("pilot_id")),
                rs.getString("author_id"),
                rs.getString("author_name"),
                PilotObservation.ObservationType.valueOf(rs.getString("type")),
                rs.getString("content"),
                rs.getString("device_id"),
                rs.getString("site_name"),
                rs.getTimestamp("created_at").toInstant());
    }

    private PilotIncident mapIncident(ResultSet rs, int rowNum) throws SQLException {
        return new PilotIncident(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("pilot_id")),
                rs.getString("reported_by"),
                PilotIncident.IncidentSeverity.valueOf(rs.getString("severity")),
                PilotIncident.IncidentCategory.valueOf(rs.getString("category")),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("device_id"),
                rs.getString("site_name"),
                rs.getString("resolution"),
                PilotIncident.IncidentStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("reported_at").toInstant(),
                rs.getTimestamp("resolved_at") != null ? rs.getTimestamp("resolved_at").toInstant() : null);
    }

    private PilotKpiSnapshot mapKpi(ResultSet rs, int rowNum) throws SQLException {
        return new PilotKpiSnapshot(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("pilot_id")),
                rs.getDate("date").toLocalDate(),
                rs.getInt("total_devices"), rs.getInt("active_devices"), rs.getInt("offline_devices"),
                rs.getDouble("uptime_percent"), rs.getDouble("telemetry_valid_percent"),
                rs.getDouble("avg_signal_quality"), rs.getInt("alerts_generated"),
                rs.getInt("alerts_confirmed"), rs.getInt("false_positives"),
                rs.getDouble("false_positive_rate"), rs.getInt("incidents_open"),
                rs.getInt("incidents_resolved"), rs.getDouble("avg_latency_ms"),
                rs.getTimestamp("computed_at").toInstant());
    }

    private PilotSite mapSite(ResultSet rs, int rowNum) throws SQLException {
        return new PilotSite(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("pilot_id")),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("contact_name"),
                rs.getString("contact_phone"),
                rs.getString("notes"),
                rs.getTimestamp("created_at").toInstant());
    }
}
