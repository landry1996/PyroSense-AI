package com.pyrosense.notification.application.port.out;

import com.pyrosense.notification.domain.model.NotificationChannel;
import com.pyrosense.notification.domain.model.NotificationTemplateCode;

import java.util.Map;

public interface TemplateRendererPort {

    String renderSubject(String templateKey, Map<String, String> variables);

    String renderBody(String templateKey, Map<String, String> variables);

    String renderSubject(NotificationTemplateCode code, NotificationChannel channel, Map<String, String> variables);

    String renderBody(NotificationTemplateCode code, NotificationChannel channel, Map<String, String> variables);
}
