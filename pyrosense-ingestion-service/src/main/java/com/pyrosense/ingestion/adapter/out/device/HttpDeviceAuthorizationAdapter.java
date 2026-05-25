package com.pyrosense.ingestion.adapter.out.device;

import com.pyrosense.ingestion.application.port.out.DeviceAuthorizationPort;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class HttpDeviceAuthorizationAdapter implements DeviceAuthorizationPort {

    private static final Logger log = LoggerFactory.getLogger(HttpDeviceAuthorizationAdapter.class);

    private final RestClient restClient;
    private final Counter deviceServiceTimeoutCounter;
    private final Counter deviceServiceErrorCounter;

    public HttpDeviceAuthorizationAdapter(
            @Value("${pyrosense.device-service.url:http://localhost:8082}") String deviceServiceUrl,
            MeterRegistry meterRegistry) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(deviceServiceUrl)
                .requestFactory(requestFactory)
                .build();

        this.deviceServiceTimeoutCounter = Counter.builder("pyrosense.ingestion.device_service.timeouts")
                .description("Number of timeout errors when calling device-service")
                .register(meterRegistry);
        this.deviceServiceErrorCounter = Counter.builder("pyrosense.ingestion.device_service.errors")
                .description("Number of errors when calling device-service")
                .register(meterRegistry);
    }

    @Override
    public boolean isDeviceActive(DeviceId deviceId) {
        try {
            var response = restClient.get()
                    .uri("/api/v1/devices/{deviceId}", deviceId.value())
                    .retrieve()
                    .body(DeviceStatusResponse.class);
            return response != null && "ACTIVE".equals(response.status());
        } catch (ResourceAccessException e) {
            log.error("Timeout or connection failure checking device status for {}: {}", deviceId, e.getMessage());
            deviceServiceTimeoutCounter.increment();
            return false;
        } catch (RestClientException e) {
            log.error("Error calling device-service for device status {}: {}", deviceId, e.getMessage());
            deviceServiceErrorCounter.increment();
            return false;
        } catch (Exception e) {
            log.warn("Unexpected error checking device status for {}: {}", deviceId, e.getMessage());
            deviceServiceErrorCounter.increment();
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
        } catch (ResourceAccessException e) {
            log.error("Timeout or connection failure checking device ownership for {}: {}", deviceId, e.getMessage());
            deviceServiceTimeoutCounter.increment();
            return false;
        } catch (RestClientException e) {
            log.error("Error calling device-service for device ownership {}: {}", deviceId, e.getMessage());
            deviceServiceErrorCounter.increment();
            return false;
        } catch (Exception e) {
            log.warn("Unexpected error checking device ownership for {}: {}", deviceId, e.getMessage());
            deviceServiceErrorCounter.increment();
            return false;
        }
    }

    private record DeviceStatusResponse(String id, String status, String tenantId) {}
}
