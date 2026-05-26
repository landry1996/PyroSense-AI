package com.pyrosense.notification.domain;

import com.pyrosense.notification.domain.model.*;
import com.pyrosense.notification.domain.model.NotificationTemplateDefinition.PrivacyRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTemplateRegistryTest {

    @ParameterizedTest
    @EnumSource(NotificationTemplateCode.class)
    void everyCodeHasARegisteredTemplate(NotificationTemplateCode code) {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(code);
        assertNotNull(def);
        assertEquals(code, def.code());
        assertNotNull(def.title());
        assertFalse(def.title().isBlank());
    }

    @ParameterizedTest
    @EnumSource(NotificationTemplateCode.class)
    void everyTemplateHasAllFourChannels(NotificationTemplateCode code) {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(code);
        assertNotNull(def.dashboard());
        assertNotNull(def.email());
        assertNotNull(def.sms());
        assertNotNull(def.push());
        assertFalse(def.dashboard().subject().isBlank());
        assertFalse(def.dashboard().body().isBlank());
        assertFalse(def.email().subject().isBlank());
        assertFalse(def.email().body().isBlank());
        assertFalse(def.sms().subject().isBlank());
        assertFalse(def.sms().body().isBlank());
        assertFalse(def.push().subject().isBlank());
        assertFalse(def.push().body().isBlank());
    }

    @ParameterizedTest
    @EnumSource(NotificationTemplateCode.class)
    void everyTemplateHasPriorityAndCallToAction(NotificationTemplateCode code) {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(code);
        assertNotNull(def.priority());
        assertNotNull(def.callToAction());
        assertFalse(def.callToAction().isBlank());
    }

    @ParameterizedTest
    @EnumSource(NotificationTemplateCode.class)
    void everyTemplateHasPrivacyRules(NotificationTemplateCode code) {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(code);
        assertNotNull(def.privacyRules());
        assertFalse(def.privacyRules().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(NotificationTemplateCode.class)
    void everyTemplateDeclaresRequiredVariables(NotificationTemplateCode code) {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(code);
        assertNotNull(def.requiredVariables());
        assertFalse(def.requiredVariables().isEmpty());
    }

    @Test
    void alertCriticalShouldNotPanicUser() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.ALERT_CRITICAL);
        String emailBody = def.email().body();

        assertFalse(emailBody.contains("incendie va se produire"));
        assertFalse(emailBody.contains("feu imminent"));
        assertTrue(emailBody.contains("ne garantit pas"));
        assertTrue(emailBody.contains("professionnel qualifié"));
    }

    @Test
    void alertCriticalShouldRecommendClearAction() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.ALERT_CRITICAL);
        String emailBody = def.email().body();

        assertTrue(emailBody.contains("Contactez un professionnel qualifié"));
        assertTrue(emailBody.contains("RECOMMANDATIONS"));
        assertEquals("Contacter un professionnel qualifié", def.callToAction());
    }

    @Test
    void alertCriticalShouldIndicateBuilding() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.ALERT_CRITICAL);
        String emailBody = def.email().body();

        assertTrue(emailBody.contains("{buildingId}"));
    }

    @Test
    void alertCriticalShouldNotShowRawElectricalData() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.ALERT_CRITICAL);

        assertTrue(def.privacyRules().contains(PrivacyRule.NO_RAW_ELECTRICAL_DATA));
        assertFalse(def.email().body().contains("ampère"));
        assertFalse(def.email().body().contains("volt"));
        assertFalse(def.email().body().contains("THD"));
        assertFalse(def.email().body().contains("kW"));
    }

    @Test
    void alertCriticalShouldNotGuaranteeFire() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.ALERT_CRITICAL);

        assertTrue(def.privacyRules().contains(PrivacyRule.NO_GUARANTEE_FIRE_PREDICTION));
        assertTrue(def.email().body().contains("ne garantit pas la survenue d'un incident"));
    }

    @Test
    void criticalRiskShouldNotGuaranteeFire() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.CRITICAL_RISK_DETECTED);

        assertTrue(def.privacyRules().contains(PrivacyRule.NO_GUARANTEE_FIRE_PREDICTION));
        assertTrue(def.email().body().contains("ne garantit pas"));
    }

    @Test
    void smsTemplatesShouldBeShort() {
        for (var entry : NotificationTemplateRegistry.all().entrySet()) {
            String smsBody = entry.getValue().sms().body();
            assertTrue(smsBody.length() <= 160,
                    "SMS body for %s is %d chars (max 160)".formatted(entry.getKey(), smsBody.length()));
        }
    }

    @Test
    void dashboardTemplatesShouldBeShort() {
        for (var entry : NotificationTemplateRegistry.all().entrySet()) {
            String dashBody = entry.getValue().dashboard().body();
            assertTrue(dashBody.length() <= 200,
                    "Dashboard body for %s is %d chars (max 200)".formatted(entry.getKey(), dashBody.length()));
        }
    }

    @Test
    void emailTemplatesShouldBeLonger() {
        for (var entry : NotificationTemplateRegistry.all().entrySet()) {
            String emailBody = entry.getValue().email().body();
            assertTrue(emailBody.length() > 100,
                    "Email body for %s should be detailed (got %d chars)".formatted(entry.getKey(), emailBody.length()));
        }
    }

    @Test
    void renderShouldSubstituteVariables() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.ALERT_WARNING);
        Map<String, String> vars = Map.of(
                "alertType", "TEMPERATURE_RISE",
                "deviceId", "sensor-042",
                "buildingId", "Immeuble Haussmann",
                "occurredAt", "2025-06-15T14:30:00Z"
        );

        String rendered = def.render(NotificationChannel.EMAIL, vars);
        assertTrue(rendered.contains("TEMPERATURE_RISE"));
        assertTrue(rendered.contains("Immeuble Haussmann"));
        assertTrue(rendered.contains("sensor-042"));
        assertTrue(rendered.contains("2025-06-15T14:30:00Z"));
        assertFalse(rendered.contains("{alertType}"));
        assertFalse(rendered.contains("{deviceId}"));
    }

    @Test
    void renderSubjectShouldSubstituteVariables() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.DEVICE_OFFLINE);
        Map<String, String> vars = Map.of(
                "deviceId", "dev-99",
                "buildingId", "B1",
                "occurredAt", "now"
        );

        String subject = def.renderSubject(NotificationChannel.DASHBOARD, vars);
        assertTrue(subject.contains("dev-99"));
    }

    @Test
    void missingVariablesShouldBeDetected() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.ALERT_CRITICAL);
        Map<String, String> incomplete = Map.of("alertType", "MICRO_ARC");

        List<String> missing = def.missingVariables(incomplete);
        assertTrue(missing.contains("buildingId"));
        assertTrue(missing.contains("occurredAt"));
        assertFalse(missing.contains("alertType"));
    }

    @Test
    void missingVariablesReturnsEmptyWhenAllPresent() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.DEVICE_BACK_ONLINE);
        Map<String, String> complete = Map.of(
                "deviceId", "d1",
                "buildingId", "b1",
                "occurredAt", "2025-01-01"
        );

        List<String> missing = def.missingVariables(complete);
        assertTrue(missing.isEmpty());
    }

    @Test
    void nullVariableValueShouldBeConsideredMissing() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.ALERT_WARNING);
        Map<String, String> vars = new HashMap<>();
        vars.put("alertType", "TEST");
        vars.put("deviceId", null);
        vars.put("buildingId", "B1");
        vars.put("occurredAt", "now");

        List<String> missing = def.missingVariables(vars);
        assertTrue(missing.contains("deviceId"));
    }

    @Test
    void renderWithMissingVariableLeavesPlaceholderEmpty() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.DEVICE_OFFLINE);
        Map<String, String> vars = Map.of(
                "deviceId", "dev-1",
                "occurredAt", "now"
        );

        String rendered = def.render(NotificationChannel.DASHBOARD, vars);
        assertTrue(rendered.contains("dev-1"));
        assertTrue(rendered.contains("{buildingId}"));
    }

    @Test
    void deviceBackOnlineTemplateExists() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.DEVICE_BACK_ONLINE);
        assertEquals(NotificationPriority.LOW, def.priority());
        assertTrue(def.email().body().contains("de nouveau opérationnel"));
        assertTrue(def.email().body().toLowerCase().contains("surveillance"));
    }

    @Test
    void contentForWebhookFallsBackToEmail() {
        NotificationTemplateDefinition def = NotificationTemplateRegistry.get(NotificationTemplateCode.REPORT_GENERATED);
        assertEquals(def.email(), def.contentFor(NotificationChannel.WEBHOOK));
    }

    @Test
    void priorityMappingIsCorrect() {
        assertEquals(NotificationPriority.URGENT, NotificationTemplateCode.ALERT_CRITICAL.priority());
        assertEquals(NotificationPriority.URGENT, NotificationTemplateCode.CRITICAL_RISK_DETECTED.priority());
        assertEquals(NotificationPriority.HIGH, NotificationTemplateCode.DEVICE_OFFLINE.priority());
        assertEquals(NotificationPriority.HIGH, NotificationTemplateCode.INTERVENTION_ASSIGNED.priority());
        assertEquals(NotificationPriority.MEDIUM, NotificationTemplateCode.ALERT_WARNING.priority());
        assertEquals(NotificationPriority.MEDIUM, NotificationTemplateCode.INTERVENTION_CREATED.priority());
        assertEquals(NotificationPriority.LOW, NotificationTemplateCode.INTERVENTION_COMPLETED.priority());
        assertEquals(NotificationPriority.LOW, NotificationTemplateCode.REPORT_GENERATED.priority());
        assertEquals(NotificationPriority.LOW, NotificationTemplateCode.DEVICE_BACK_ONLINE.priority());
    }

    @Test
    void fromAlertSeverityMapsCorrectly() {
        assertEquals(NotificationTemplateCode.ALERT_CRITICAL,
                NotificationTemplateCode.fromAlertSeverity(com.pyrosense.shared.valueobject.AlertSeverity.CRITICAL));
        assertEquals(NotificationTemplateCode.ALERT_WARNING,
                NotificationTemplateCode.fromAlertSeverity(com.pyrosense.shared.valueobject.AlertSeverity.WARNING));
    }
}
