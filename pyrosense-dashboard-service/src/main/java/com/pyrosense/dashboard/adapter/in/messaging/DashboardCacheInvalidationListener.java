package com.pyrosense.dashboard.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.dashboard.application.port.out.DashboardCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DashboardCacheInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(DashboardCacheInvalidationListener.class);

    private final DashboardCachePort cache;
    private final ObjectMapper objectMapper;

    public DashboardCacheInvalidationListener(DashboardCachePort cache, ObjectMapper objectMapper) {
        this.cache = cache;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {
            "risk-score-updated",
            "alert-created",
            "alert-resolved",
            "intervention-completed",
            "device-offline-detected"
    }, groupId = "dashboard-cache-invalidation")
    public void onEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String tenantId = extractTenantId(node);
            if (tenantId == null) {
                return;
            }
            String prefix = "dashboard:" ;
            cache.evictByPrefix(prefix + "overview:" + tenantId);
            cache.evictByPrefix(prefix + "risky-buildings:" + tenantId);
            cache.evictByPrefix(prefix + "risk-trend:" + tenantId);
            cache.evictByPrefix(prefix + "device-health:" + tenantId);
            log.debug("Cache invalidated for tenant={}", tenantId);
        } catch (Exception e) {
            log.warn("Failed to process cache invalidation event: {}", e.getMessage());
        }
    }

    private String extractTenantId(JsonNode node) {
        if (node.has("tenantId")) {
            return node.get("tenantId").asText();
        }
        if (node.has("tenant_id")) {
            return node.get("tenant_id").asText();
        }
        if (node.has("data") && node.get("data").has("tenantId")) {
            return node.get("data").get("tenantId").asText();
        }
        return null;
    }
}
