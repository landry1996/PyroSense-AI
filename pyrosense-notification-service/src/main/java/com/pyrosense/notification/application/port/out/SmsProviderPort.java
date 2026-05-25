package com.pyrosense.notification.application.port.out;

public interface SmsProviderPort {
    void send(String phoneNumber, String message);
}
