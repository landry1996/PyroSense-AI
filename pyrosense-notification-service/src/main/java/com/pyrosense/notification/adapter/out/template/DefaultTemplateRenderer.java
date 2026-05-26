package com.pyrosense.notification.adapter.out.template;

import com.pyrosense.notification.application.port.out.TemplateRendererPort;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultTemplateRenderer implements TemplateRendererPort {

    private static final Map<String, TemplateEntry> TEMPLATES = Map.ofEntries(
            Map.entry("alert.critical", new TemplateEntry(
                    "[CRITIQUE] Alerte {alertType} — Capteur {deviceId}",
                    "Une alerte CRITIQUE de type {alertType} a été détectée sur le capteur {deviceId} à {occurredAt}. Une intervention immédiate est recommandée.")),
            Map.entry("alert.warning", new TemplateEntry(
                    "[ATTENTION] Alerte {alertType} — Capteur {deviceId}",
                    "Une alerte de type {alertType} a été détectée sur le capteur {deviceId} à {occurredAt}. Surveillance renforcée recommandée.")),
            Map.entry("alert.info", new TemplateEntry(
                    "[INFO] Notification {alertType}",
                    "Information : un événement de type {alertType} a été enregistré sur le capteur {deviceId} à {occurredAt}.")),
            Map.entry("intervention.created", new TemplateEntry(
                    "Nouvelle intervention — {type}",
                    "Une intervention de type {type} a été créée pour le bâtiment {buildingId}.")),
            Map.entry("intervention.assigned", new TemplateEntry(
                    "Intervention assignée — {type}",
                    "Vous avez été assigné à une intervention de type {type} sur le bâtiment {buildingId}.")),
            Map.entry("intervention.completed", new TemplateEntry(
                    "Intervention terminée — {type}",
                    "L'intervention de type {type} sur le bâtiment {buildingId} a été complétée.")),
            Map.entry("report.generated", new TemplateEntry(
                    "Rapport disponible — {reportType} ({reportNumber})",
                    "Le rapport {reportType} N° {reportNumber} est prêt pour consultation.")),
            Map.entry("device.offline", new TemplateEntry(
                    "[ALERTE] Capteur hors ligne — {deviceId}",
                    "Le capteur {deviceId} du bâtiment {buildingId} est détecté hors ligne. Vérifiez la connectivité.")),
            Map.entry("risk.critical", new TemplateEntry(
                    "[CRITIQUE] Score de risque critique — {deviceId}",
                    "Le capteur {deviceId} du bâtiment {buildingId} a atteint un score de risque de {riskScore}/100. Action immédiate recommandée."))
    );

    @Override
    public String renderSubject(String templateKey, Map<String, String> variables) {
        TemplateEntry entry = TEMPLATES.getOrDefault(templateKey, TEMPLATES.get("alert.info"));
        return substitute(entry.subject(), variables);
    }

    @Override
    public String renderBody(String templateKey, Map<String, String> variables) {
        TemplateEntry entry = TEMPLATES.getOrDefault(templateKey, TEMPLATES.get("alert.info"));
        return substitute(entry.body(), variables);
    }

    private String substitute(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
        }
        return result;
    }

    private record TemplateEntry(String subject, String body) {}
}
