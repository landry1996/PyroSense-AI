package com.pyrosense.notification.adapter.out.template;

import com.pyrosense.notification.domain.model.NotificationChannel;
import com.pyrosense.notification.domain.model.NotificationTemplateCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTemplateRendererTest {

    private final DefaultTemplateRenderer renderer = new DefaultTemplateRenderer();

    @Test
    void legacyRenderSubjectStillWorks() {
        Map<String, String> vars = Map.of(
                "alertType", "OVERLOAD",
                "deviceId", "dev-001"
        );
        String subject = renderer.renderSubject("alert.warning", vars);
        assertTrue(subject.contains("OVERLOAD"));
        assertTrue(subject.contains("dev-001"));
    }

    @Test
    void legacyRenderBodyStillWorks() {
        Map<String, String> vars = Map.of(
                "alertType", "MICRO_ARC",
                "deviceId", "dev-002",
                "occurredAt", "2025-03-10T08:00:00Z"
        );
        String body = renderer.renderBody("alert.critical", vars);
        assertTrue(body.contains("MICRO_ARC"));
        assertTrue(body.contains("dev-002"));
    }

    @Test
    void legacyUnknownKeyFallsBackToInfo() {
        Map<String, String> vars = Map.of("alertType", "TEST", "deviceId", "d1", "occurredAt", "now");
        String subject = renderer.renderSubject("unknown.key", vars);
        assertTrue(subject.contains("TEST"));
    }

    @ParameterizedTest
    @EnumSource(NotificationTemplateCode.class)
    void channelAwareRenderSubjectWorks(NotificationTemplateCode code) {
        Map<String, String> vars = Map.of(
                "alertType", "TEMPERATURE_RISE",
                "deviceId", "dev-100",
                "buildingId", "Bat-A",
                "occurredAt", "2025-06-01",
                "riskScore", "87",
                "interventionType", "CORRECTIVE",
                "reportType", "MONTHLY_HEALTH",
                "reportNumber", "MH-202506-00001"
        );

        String subject = renderer.renderSubject(code, NotificationChannel.EMAIL, vars);
        assertNotNull(subject);
        assertFalse(subject.isBlank());
    }

    @ParameterizedTest
    @EnumSource(NotificationTemplateCode.class)
    void channelAwareRenderBodyWorks(NotificationTemplateCode code) {
        Map<String, String> vars = Map.of(
                "alertType", "LOOSE_CONNECTION",
                "deviceId", "dev-200",
                "buildingId", "Bat-B",
                "occurredAt", "2025-07-01",
                "riskScore", "92",
                "interventionType", "PREVENTIVE",
                "reportType", "MONITORING_CERTIFICATE",
                "reportNumber", "MC-202507-00003"
        );

        String body = renderer.renderBody(code, NotificationChannel.DASHBOARD, vars);
        assertNotNull(body);
        assertFalse(body.isBlank());
        assertFalse(body.contains("{alertType}") || body.contains("{deviceId}") || body.contains("{buildingId}"));
    }

    @Test
    void emailBodyIsLongerThanSmsDashboard() {
        Map<String, String> vars = Map.of(
                "alertType", "MICRO_ARC",
                "buildingId", "Bat-C",
                "occurredAt", "2025-01-15"
        );

        String emailBody = renderer.renderBody(NotificationTemplateCode.ALERT_CRITICAL, NotificationChannel.EMAIL, vars);
        String smsBody = renderer.renderBody(NotificationTemplateCode.ALERT_CRITICAL, NotificationChannel.SMS, vars);
        String dashBody = renderer.renderBody(NotificationTemplateCode.ALERT_CRITICAL, NotificationChannel.DASHBOARD, vars);

        assertTrue(emailBody.length() > smsBody.length());
        assertTrue(emailBody.length() > dashBody.length());
    }

    @Test
    void criticalAlertEmailContainsSafetyDisclaimer() {
        Map<String, String> vars = Map.of(
                "alertType", "ARC",
                "buildingId", "B1",
                "occurredAt", "now"
        );

        String body = renderer.renderBody(NotificationTemplateCode.ALERT_CRITICAL, NotificationChannel.EMAIL, vars);
        assertTrue(body.contains("aide à la décision"));
        assertTrue(body.contains("professionnel qualifié"));
    }
}
