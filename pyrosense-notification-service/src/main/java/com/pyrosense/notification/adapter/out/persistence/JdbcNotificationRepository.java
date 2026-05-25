package com.pyrosense.notification.adapter.out.persistence;

import com.pyrosense.notification.application.port.out.NotificationRepositoryPort;
import com.pyrosense.notification.domain.model.Notification;
import com.pyrosense.notification.domain.model.NotificationChannel;
import com.pyrosense.notification.domain.model.NotificationStatus;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcNotificationRepository implements NotificationRepositoryPort {

    private final JdbcTemplate jdbc;
    private final NotificationRowMapper rowMapper = new NotificationRowMapper();

    public JdbcNotificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Notification save(Notification n) {
        int updated = jdbc.update("""
                UPDATE notifications SET status = ?, retry_count = ?, next_retry_at = ?,
                    sent_at = ?, failure_reason = ?
                WHERE id = ?
                """,
                n.getStatus().name(),
                n.getRetryCount(),
                toTimestamp(n.getNextRetryAt()),
                toTimestamp(n.getSentAt()),
                n.getFailureReason(),
                n.getId());

        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO notifications (id, tenant_id, recipient_id, channel, severity,
                        subject, body, alert_fingerprint, status, retry_count, next_retry_at,
                        created_at, sent_at, failure_reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    n.getId(),
                    n.getTenantId().value(),
                    n.getRecipientId().value(),
                    n.getChannel().name(),
                    n.getSeverity().name(),
                    n.getSubject(),
                    n.getBody(),
                    n.getAlertFingerprint(),
                    n.getStatus().name(),
                    n.getRetryCount(),
                    toTimestamp(n.getNextRetryAt()),
                    Timestamp.from(n.getCreatedAt()),
                    toTimestamp(n.getSentAt()),
                    n.getFailureReason());
        }
        return n;
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        List<Notification> results = jdbc.query("SELECT * FROM notifications WHERE id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    @Override
    public List<Notification> findByRecipientId(UserId recipientId) {
        return jdbc.query("SELECT * FROM notifications WHERE recipient_id = ? ORDER BY created_at DESC",
                rowMapper, recipientId.value());
    }

    @Override
    public List<Notification> findByTenantId(TenantId tenantId) {
        return jdbc.query("SELECT * FROM notifications WHERE tenant_id = ? ORDER BY created_at DESC",
                rowMapper, tenantId.value());
    }

    @Override
    public List<Notification> findByStatus(NotificationStatus status) {
        return jdbc.query("SELECT * FROM notifications WHERE status = ? ORDER BY created_at DESC",
                rowMapper, status.name());
    }

    @Override
    public List<Notification> findPendingRetries() {
        return jdbc.query("""
                SELECT * FROM notifications WHERE status = 'RETRYING'
                AND next_retry_at <= ? ORDER BY next_retry_at
                """, rowMapper, Timestamp.from(Instant.now()));
    }

    @Override
    public long countByTenantIdAndStatus(TenantId tenantId, NotificationStatus status) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE tenant_id = ? AND status = ?",
                Long.class, tenantId.value(), status.name());
        return count != null ? count : 0;
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    private static class NotificationRowMapper implements RowMapper<Notification> {
        @Override
        public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
            Notification n = new Notification(
                    rs.getObject("id", UUID.class),
                    new TenantId(rs.getObject("tenant_id", UUID.class)),
                    new UserId(rs.getObject("recipient_id", UUID.class)),
                    NotificationChannel.valueOf(rs.getString("channel")),
                    AlertSeverity.valueOf(rs.getString("severity")),
                    rs.getString("subject"),
                    rs.getString("body"),
                    rs.getString("alert_fingerprint")
            );

            String status = rs.getString("status");
            int retryCount = rs.getInt("retry_count");
            String failureReason = rs.getString("failure_reason");
            Timestamp sentAt = rs.getTimestamp("sent_at");

            if ("SENT".equals(status)) {
                n.markSent();
            } else if ("FAILED".equals(status) || "RETRYING".equals(status)) {
                for (int i = 0; i < retryCount; i++) {
                    n.markFailed(failureReason);
                }
            }

            return n;
        }
    }
}
