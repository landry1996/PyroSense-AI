package com.pyrosense.notification.application.port.out;

public interface PushProviderPort {
    void send(String pushToken, String title, String body);
}
