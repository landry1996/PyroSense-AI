package com.pyrosense.notification.adapter.out.provider;

import com.pyrosense.notification.application.port.out.WebhookProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LoggingWebhookProvider implements WebhookProviderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingWebhookProvider.class);

    @Override
    public void send(String webhookUrl, Map<String, Object> payload) {
        log.info("[WEBHOOK] URL: {} | Payload keys: {}", webhookUrl, payload.keySet());
    }
}
