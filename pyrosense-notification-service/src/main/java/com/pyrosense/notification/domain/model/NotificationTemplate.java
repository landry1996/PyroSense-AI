package com.pyrosense.notification.domain.model;

import com.pyrosense.shared.valueobject.AlertSeverity;

import java.util.Map;

public record NotificationTemplate(
        String subjectTemplate,
        String bodyTemplate
) {
    private static final Map<AlertSeverity, NotificationTemplate> ALERT_TEMPLATES = Map.of(
            AlertSeverity.INFO, new NotificationTemplate(
                    "[PyroSense] Information - {alertType}",
                    "Bonjour,\n\nUne information a été enregistrée sur votre installation.\n\nType : {alertType}\nBâtiment : {deviceId}\nDate : {occurredAt}\n\nAucune action immédiate requise.\n\nCordialement,\nPyroSense AI Platform"
            ),
            AlertSeverity.WARNING, new NotificationTemplate(
                    "[PyroSense] ⚠ Attention requise - {alertType}",
                    "Bonjour,\n\nUne alerte de niveau WARNING a été détectée.\n\nType : {alertType}\nAppareil : {deviceId}\nDate : {occurredAt}\n\nAction recommandée dans les 48 heures.\n\nCordialement,\nPyroSense AI Platform"
            ),
            AlertSeverity.CRITICAL, new NotificationTemplate(
                    "[PyroSense] 🚨 CRITIQUE - Intervention immédiate requise - {alertType}",
                    "ALERTE CRITIQUE\n\nUne situation critique a été détectée nécessitant une intervention immédiate.\n\nType : {alertType}\nAppareil : {deviceId}\nDate : {occurredAt}\n\nVeuillez intervenir immédiatement ou contacter le support.\n\nPyroSense AI Platform"
            )
    );

    public static NotificationTemplate forSeverity(AlertSeverity severity) {
        return ALERT_TEMPLATES.get(severity);
    }

    public String renderSubject(Map<String, String> variables) {
        return render(subjectTemplate, variables);
    }

    public String renderBody(Map<String, String> variables) {
        return render(bodyTemplate, variables);
    }

    private String render(String template, Map<String, String> variables) {
        String result = template;
        for (var entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
