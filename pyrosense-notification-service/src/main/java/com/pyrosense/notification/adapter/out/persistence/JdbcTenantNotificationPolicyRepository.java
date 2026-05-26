package com.pyrosense.notification.adapter.out.persistence;

import com.pyrosense.notification.application.port.out.TenantNotificationPolicyRepository;
import com.pyrosense.notification.domain.model.TenantNotificationPolicy;
import com.pyrosense.shared.id.TenantId;
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
public class JdbcTenantNotificationPolicyRepository implements TenantNotificationPolicyRepository {

    private final JdbcTemplate jdbc;

    public JdbcTenantNotificationPolicyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<TenantNotificationPolicy> findByTenantId(TenantId tenantId) {
        List<TenantNotificationPolicy> results = jdbc.query(
                "SELECT * FROM tenant_notification_policy WHERE tenant_id = ?",
                (rs, rowNum) -> TenantNotificationPolicy.reconstitute(
                        new TenantId(UUID.fromString(rs.getString("tenant_id"))),
                        rs.getBoolean("email_enabled_by_default"),
                        rs.getBoolean("sms_enabled_by_default"),
                        rs.getBoolean("push_enabled_by_default"),
                        rs.getBoolean("webhook_enabled_by_default"),
                        rs.getBoolean("critical_override_mandatory"),
                        rs.getBoolean("require_phone_verification"),
                        rs.getBoolean("require_push_token"),
                        toLocalTime(rs.getTime("default_quiet_hours_start")),
                        toLocalTime(rs.getTime("default_quiet_hours_end")),
                        rs.getString("default_language"),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at"))
                ),
                tenantId.value().toString()
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public TenantNotificationPolicy save(TenantNotificationPolicy policy) {
        int updated = jdbc.update("""
            UPDATE tenant_notification_policy
            SET email_enabled_by_default = ?, sms_enabled_by_default = ?,
                push_enabled_by_default = ?, webhook_enabled_by_default = ?,
                critical_override_mandatory = ?, require_phone_verification = ?,
                require_push_token = ?, default_quiet_hours_start = ?,
                default_quiet_hours_end = ?, default_language = ?, updated_at = NOW()
            WHERE tenant_id = ?
            """,
                policy.isEmailEnabledByDefault(), policy.isSmsEnabledByDefault(),
                policy.isPushEnabledByDefault(), policy.isWebhookEnabledByDefault(),
                policy.isCriticalOverrideMandatory(), policy.isRequirePhoneVerificationForSms(),
                policy.isRequirePushTokenForPush(),
                toTime(policy.getDefaultQuietHoursStart()), toTime(policy.getDefaultQuietHoursEnd()),
                policy.getDefaultLanguage(),
                policy.getTenantId().value().toString()
        );
        if (updated == 0) {
            jdbc.update("""
                INSERT INTO tenant_notification_policy (tenant_id, email_enabled_by_default, sms_enabled_by_default,
                    push_enabled_by_default, webhook_enabled_by_default, critical_override_mandatory,
                    require_phone_verification, require_push_token, default_quiet_hours_start,
                    default_quiet_hours_end, default_language, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                    policy.getTenantId().value().toString(),
                    policy.isEmailEnabledByDefault(), policy.isSmsEnabledByDefault(),
                    policy.isPushEnabledByDefault(), policy.isWebhookEnabledByDefault(),
                    policy.isCriticalOverrideMandatory(), policy.isRequirePhoneVerificationForSms(),
                    policy.isRequirePushTokenForPush(),
                    toTime(policy.getDefaultQuietHoursStart()), toTime(policy.getDefaultQuietHoursEnd()),
                    policy.getDefaultLanguage(),
                    Timestamp.from(policy.getCreatedAt())
            );
        }
        return policy;
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
