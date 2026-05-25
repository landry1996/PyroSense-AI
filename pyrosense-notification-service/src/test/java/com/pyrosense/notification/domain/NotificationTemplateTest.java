package com.pyrosense.notification.domain;

import com.pyrosense.notification.domain.model.NotificationTemplate;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTemplateTest {

    @ParameterizedTest
    @EnumSource(AlertSeverity.class)
    void shouldHaveTemplateForEachSeverity(AlertSeverity severity) {
        NotificationTemplate template = NotificationTemplate.forSeverity(severity);
        assertNotNull(template);
        assertNotNull(template.subjectTemplate());
        assertNotNull(template.bodyTemplate());
    }

    @Test
    void shouldRenderSubjectWithVariables() {
        NotificationTemplate template = NotificationTemplate.forSeverity(AlertSeverity.WARNING);
        Map<String, String> vars = Map.of(
                "alertType", "TEMPERATURE_RISE",
                "deviceId", "dev-001",
                "occurredAt", "2025-01-15T10:30:00Z"
        );

        String subject = template.renderSubject(vars);
        assertTrue(subject.contains("TEMPERATURE_RISE"));
        assertTrue(subject.contains("PyroSense"));
    }

    @Test
    void shouldRenderBodyWithVariables() {
        NotificationTemplate template = NotificationTemplate.forSeverity(AlertSeverity.CRITICAL);
        Map<String, String> vars = Map.of(
                "alertType", "MICRO_ARC",
                "deviceId", "dev-002",
                "occurredAt", "2025-01-15T10:30:00Z"
        );

        String body = template.renderBody(vars);
        assertTrue(body.contains("MICRO_ARC"));
        assertTrue(body.contains("dev-002"));
        assertTrue(body.contains("critique"));
    }

    @Test
    void criticalSubjectShouldContainCritique() {
        NotificationTemplate template = NotificationTemplate.forSeverity(AlertSeverity.CRITICAL);
        assertTrue(template.subjectTemplate().contains("CRITIQUE"));
    }
}
