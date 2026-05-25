package com.pyrosense.ingestion.adapter.out.device;

import com.pyrosense.ingestion.application.port.out.DeviceAuthorizationPort;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpDeviceAuthorizationAdapter implements DeviceAuthorizationPort {

    private static final Logger log = LoggerFactory.getLogger(HttpDeviceAuthorizationAdapter.class);

    private final RestClient restClient;

    public HttpDeviceAuthorizationAdapter(
            @Value("${pyrosense.device-service.url:http://localhost:8082}") String deviceServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(deviceServiceUrl)
                .build();
    }

    @Override
    public boolean isDeviceActive(DeviceId deviceId) {
        try {
            var response = restClient.get()
                    .uri("/api/v1/devices/{deviceId}", deviceId.value())
                    .retrieve()
                    .body(DeviceStatusResponse.class);
            return response != null && "ACTIVE".equals(response.status());
        } catch (Exception e) {
            log.warn("Failed to check device status for {}: {}", deviceId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isDeviceOwnedByTenant(DeviceId deviceId, TenantId tenantId) {
        try {
            var response = restClient.get()
                    .uri("/api/v1/devices/{deviceId}", deviceId.value())
                    .retrieve()
                    .body(DeviceStatusResponse.class);
            return response != null && tenantId.toString().equals(response.tenantId());
        } catch (Exception e) {
            log.warn("Failed to check device ownership for {}: {}", deviceId, e.getMessage());
            return false;
        }
    }

    private record DeviceStatusResponse(String id, String status, String tenantId) {}
}
