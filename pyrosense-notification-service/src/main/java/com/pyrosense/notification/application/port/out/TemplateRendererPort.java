package com.pyrosense.notification.application.port.out;

import java.util.Map;

public interface TemplateRendererPort {

    String renderSubject(String templateKey, Map<String, String> variables);

    String renderBody(String templateKey, Map<String, String> variables);
}
