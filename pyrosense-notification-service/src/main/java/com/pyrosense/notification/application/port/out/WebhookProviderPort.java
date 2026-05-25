package com.pyrosense.notification.application.port.out;

import java.util.Map;

public interface WebhookProviderPort {
    void send(String webhookUrl, Map<String, Object> payload);
}
