package com.pyrosense.notification.application.port.out;

public interface EmailProviderPort {
    void send(String to, String subject, String body);
}
