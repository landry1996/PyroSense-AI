package com.pyrosense.notification.adapter.in.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class WebSocketEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventBroadcaster.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public WebSocketEventBroadcaster(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void broadcastAlert(String tenantId, String eventType, String jsonPayload) {
        log.debug("Broadcasting alert to tenant {}", tenantId);
        String message = wrapEvent(eventType, jsonPayload);
        messagingTemplate.convertAndSend("/topic/tenant." + tenantId + ".alerts", message);
    }

    public void broadcastTelemetry(String tenantId, String deviceId, String jsonPayload) {
        String message = wrapEvent("TELEMETRY_RECEIVED", jsonPayload);
        messagingTemplate.convertAndSend("/topic/tenant." + tenantId + ".telemetry." + deviceId, message);
    }

    public void broadcastDashboardUpdate(String tenantId, String eventType, String jsonPayload) {
        String message = wrapEvent(eventType, jsonPayload);
        messagingTemplate.convertAndSend("/topic/tenant." + tenantId + ".dashboard", message);
    }

    public void broadcastFromKafkaEvent(String eventType, String tenantId, String payload) {
        switch (eventType) {
            case "alerting.alert.created", "alerting.alert.escalated", "alerting.alert.resolved" ->
                    broadcastAlert(tenantId, eventType, payload);
            case "telemetry.received" ->
                    broadcastTelemetry(tenantId, extractDeviceId(payload), payload);
            case "risk.score.updated", "device.status.changed" ->
                    broadcastDashboardUpdate(tenantId, eventType, payload);
            default -> log.trace("Unhandled event type for WebSocket broadcast: {}", eventType);
        }
    }

    private String wrapEvent(String eventType, String jsonPayload) {
        try {
            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.put("eventType", eventType);
            wrapper.put("timestamp", Instant.now().toString());
            wrapper.set("data", objectMapper.readTree(jsonPayload));
            return objectMapper.writeValueAsString(wrapper);
        } catch (Exception e) {
            log.warn("Failed to wrap event payload, sending raw: {}", e.getMessage());
            return jsonPayload;
        }
    }

    private String extractDeviceId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("deviceId")) {
                JsonNode deviceId = node.get("deviceId");
                return deviceId.isObject() && deviceId.has("value")
                        ? deviceId.get("value").asText()
                        : deviceId.asText();
            }
        } catch (Exception e) {
            log.warn("Failed to extract deviceId from payload");
        }
        return "unknown";
    }
}
