package com.pyrosense.notification.adapter.out.provider;

import com.pyrosense.notification.application.port.out.PushProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPushProvider implements PushProviderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushProvider.class);

    @Override
    public void send(String pushToken, String title, String body) {
        log.info("[PUSH] Token: {} | Title: {}", maskToken(pushToken), title);
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) return "***";
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}
