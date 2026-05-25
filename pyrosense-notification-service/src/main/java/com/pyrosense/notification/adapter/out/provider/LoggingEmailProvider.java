package com.pyrosense.notification.adapter.out.provider;

import com.pyrosense.notification.application.port.out.EmailProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmailProvider implements EmailProviderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailProvider.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[EMAIL] To: {} | Subject: {}", maskEmail(to), subject);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIdx = email.indexOf('@');
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 2) return "**" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
