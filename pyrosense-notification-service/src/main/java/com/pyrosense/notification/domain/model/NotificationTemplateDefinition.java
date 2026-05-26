package com.pyrosense.notification.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record NotificationTemplateDefinition(
        NotificationTemplateCode code,
        String title,
        ChannelContent dashboard,
        ChannelContent email,
        ChannelContent sms,
        ChannelContent push,
        NotificationPriority priority,
        String callToAction,
        Set<PrivacyRule> privacyRules,
        List<String> requiredVariables
) {

    public record ChannelContent(String subject, String body) {}

    public enum PrivacyRule {
        NO_RAW_ELECTRICAL_DATA,
        NO_EXACT_LOCATION,
        NO_PERSONAL_INFO_IN_PUSH,
        MASK_DEVICE_ID_IN_SMS,
        NO_GUARANTEE_FIRE_PREDICTION
    }

    public ChannelContent contentFor(NotificationChannel channel) {
        return switch (channel) {
            case DASHBOARD -> dashboard;
            case EMAIL -> email;
            case SMS -> sms;
            case PUSH -> push;
            case WEBHOOK -> email;
        };
    }

    public String render(NotificationChannel channel, Map<String, String> variables) {
        ChannelContent content = contentFor(channel);
        return substitute(content.body(), variables);
    }

    public String renderSubject(NotificationChannel channel, Map<String, String> variables) {
        ChannelContent content = contentFor(channel);
        return substitute(content.subject(), variables);
    }

    private String substitute(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
        }
        return result;
    }

    public List<String> missingVariables(Map<String, String> variables) {
        return requiredVariables.stream()
                .filter(v -> !variables.containsKey(v) || variables.get(v) == null)
                .toList();
    }
}
