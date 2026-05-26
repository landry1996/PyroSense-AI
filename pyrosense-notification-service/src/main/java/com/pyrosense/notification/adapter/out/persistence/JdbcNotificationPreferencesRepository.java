package com.pyrosense.notification.adapter.out.persistence;

import com.pyrosense.notification.application.port.out.NotificationPreferencesRepository;
import com.pyrosense.notification.domain.model.NotificationPreferences;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcNotificationPreferencesRepository implements NotificationPreferencesRepository {

    private final JdbcTemplate jdbc;

    public JdbcNotificationPreferencesRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<NotificationPreferences> findByUserId(UserId userId) {
        List<NotificationPreferences> results = jdbc.query(
                "SELECT * FROM notification_preferences WHERE user_id = ?",
                (rs, rowNum) -> NotificationPreferences.reconstitute(
                        new UserId(UUID.fromString(rs.getString("user_id"))),
                        new TenantId(UUID.fromString(rs.getString("tenant_id"))),
                        rs.getBoolean("email_enabled"),
                        rs.getBoolean("sms_enabled"),
                        rs.getBoolean("push_enabled"),
                        rs.getBoolean("webhook_enabled"),
                        toLocalTime(rs.getTime("quiet_hours_start")),
                        toLocalTime(rs.getTime("quiet_hours_end")),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at"))
                ),
                userId.value().toString()
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public NotificationPreferences save(NotificationPreferences prefs) {
        int updated = jdbc.update("""
            UPDATE notification_preferences
            SET email_enabled = ?, sms_enabled = ?, push_enabled = ?, webhook_enabled = ?,
                quiet_hours_start = ?, quiet_hours_end = ?, updated_at = NOW()
            WHERE user_id = ?
            """,
                prefs.isEmailEnabled(), prefs.isSmsEnabled(), prefs.isPushEnabled(), prefs.isWebhookEnabled(),
                toTime(prefs.getQuietHoursStart()), toTime(prefs.getQuietHoursEnd()),
                prefs.getUserId().value().toString()
        );
        if (updated == 0) {
            jdbc.update("""
                INSERT INTO notification_preferences (user_id, tenant_id, email_enabled, sms_enabled,
                    push_enabled, webhook_enabled, quiet_hours_start, quiet_hours_end, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                    prefs.getUserId().value().toString(),
                    prefs.getTenantId().value().toString(),
                    prefs.isEmailEnabled(), prefs.isSmsEnabled(), prefs.isPushEnabled(), prefs.isWebhookEnabled(),
                    toTime(prefs.getQuietHoursStart()), toTime(prefs.getQuietHoursEnd()),
                    Timestamp.from(prefs.getCreatedAt())
            );
        }
        return prefs;
    }

    private static LocalTime toLocalTime(Time time) {
        return time != null ? time.toLocalTime() : null;
    }

    private static Time toTime(LocalTime localTime) {
        return localTime != null ? Time.valueOf(localTime) : null;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
