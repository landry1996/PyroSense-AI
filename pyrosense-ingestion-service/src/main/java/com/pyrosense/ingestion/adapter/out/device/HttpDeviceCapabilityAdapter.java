package com.pyrosense.ingestion.adapter.out.device;

import com.pyrosense.ingestion.application.port.out.DeviceCapabilityLookupPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HttpDeviceCapabilityAdapter implements DeviceCapabilityLookupPort {

    private static final Logger log = LoggerFactory.getLogger(HttpDeviceCapabilityAdapter.class);
    private static final long CACHE_TTL_MS = 60_000;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final RestClient restClient;

    private record CacheEntry(DeviceCapability capability, long expiresAt) {
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    public HttpDeviceCapabilityAdapter(
            @Value("${pyrosense.device-service.url:http://localhost:8082}") String deviceServiceUrl) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(deviceServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<DeviceCapability> getCapability(String deviceId) {
        CacheEntry entry = cache.get(deviceId);
        if (entry != null && !entry.isExpired()) {
            return Optional.ofNullable(entry.capability());
        }

        try {
            var response = restClient.get()
                    .uri("/internal/devices/{deviceId}/capability", deviceId)
                    .retrieve()
                    .body(CapabilityResponse.class);

            if (response == null) {
                cache.put(deviceId, new CacheEntry(null, System.currentTimeMillis() + CACHE_TTL_MS));
                return Optional.empty();
            }

            var capability = new DeviceCapability(
                    deviceId, response.tenantId(), response.deviceModel(),
                    response.firmwareVersion(), response.supportsSignature(),
                    response.supportsAntiReplay(), response.maxSamplingRateMs());
            cache.put(deviceId, new CacheEntry(capability, System.currentTimeMillis() + CACHE_TTL_MS));
            return Optional.of(capability);
        } catch (Exception e) {
            log.debug("Error fetching device capability for {}: {}", deviceId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean isRealDevice(String deviceId) {
        return getCapability(deviceId)
                .map(DeviceCapability::supportsSignature)
                .orElse(false);
    }

    private record CapabilityResponse(
            String tenantId, String deviceModel, String firmwareVersion,
            boolean supportsSignature, boolean supportsAntiReplay, int maxSamplingRateMs) {}
}
