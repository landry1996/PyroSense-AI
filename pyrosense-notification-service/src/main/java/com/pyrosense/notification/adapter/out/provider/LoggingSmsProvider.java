package com.pyrosense.notification.adapter.out.provider;

import com.pyrosense.notification.application.port.out.SmsProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingSmsProvider implements SmsProviderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsProvider.class);

    @Override
    public void send(String phoneNumber, String message) {
        log.info("[SMS] To: {} | Message length: {}", maskPhone(phoneNumber), message.length());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}
