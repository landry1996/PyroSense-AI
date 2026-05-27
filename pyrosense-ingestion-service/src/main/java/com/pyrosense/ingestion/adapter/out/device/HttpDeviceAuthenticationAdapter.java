package com.pyrosense.ingestion.adapter.out.device;

import com.pyrosense.ingestion.application.port.out.DeviceAuthenticationPort;
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
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HttpDeviceAuthenticationAdapter implements DeviceAuthenticationPort {

    private static final Logger log = LoggerFactory.getLogger(HttpDeviceAuthenticationAdapter.class);
    private static final long CACHE_TTL_MS = 30_000;

    private final ConcurrentHashMap<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private final RestClient restClient;
    private final Counter authCheckCounter;
    private final Counter authErrorCounter;

    private record CacheEntry<T>(T value, long expiresAt) {
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    public HttpDeviceAuthenticationAdapter(
            @Value("${pyrosense.device-service.url:http://localhost:8082}") String deviceServiceUrl,
            MeterRegistry meterRegistry) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(deviceServiceUrl)
                .requestFactory(requestFactory)
                .build();

        this.authCheckCounter = Counter.builder("pyrosense.ingestion.device_auth.checks")
                .register(meterRegistry);
        this.authErrorCounter = Counter.builder("pyrosense.ingestion.device_auth.errors")
                .register(meterRegistry);
    }

    @Override
    public DeviceStatus checkDeviceStatus(String deviceId, String tenantId) {
        authCheckCounter.increment();
        String cacheKey = "status:" + deviceId + ":" + tenantId;
        @SuppressWarnings("unchecked")
        CacheEntry<DeviceStatus> entry = (CacheEntry<DeviceStatus>) cache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            return entry.value();
        }

        try {
            var response = restClient.get()
                    .uri("/internal/devices/{deviceId}/auth-status?tenantId={tenantId}", deviceId, tenantId)
                    .retrieve()
                    .body(DeviceAuthResponse.class);

            DeviceStatus status;
            if (response == null) {
                status = DeviceStatus.NOT_FOUND;
            } else if ("REVOKED".equals(response.status())) {
                status = DeviceStatus.REVOKED;
            } else if ("ACTIVE".equals(response.status()) || "PROVISIONED".equals(response.status())) {
                status = DeviceStatus.ACTIVE;
            } else {
                status = DeviceStatus.NOT_FOUND;
            }

            cache.put(cacheKey, new CacheEntry<>(status, System.currentTimeMillis() + CACHE_TTL_MS));
            return status;
        } catch (ResourceAccessException e) {
            log.error("Timeout checking device auth for {}: {}", deviceId, e.getMessage());
            authErrorCounter.increment();
            return DeviceStatus.NOT_FOUND;
        } catch (RestClientException e) {
            log.error("Error checking device auth for {}: {}", deviceId, e.getMessage());
            authErrorCounter.increment();
            return DeviceStatus.NOT_FOUND;
        }
    }

    @Override
    public byte[] getActiveHmacKey(String deviceId) {
        String cacheKey = "hmac:" + deviceId;
        @SuppressWarnings("unchecked")
        CacheEntry<byte[]> entry = (CacheEntry<byte[]>) cache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            return entry.value();
        }

        try {
            var response = restClient.get()
                    .uri("/internal/devices/{deviceId}/hmac-key", deviceId)
                    .retrieve()
                    .body(HmacKeyResponse.class);

            if (response == null || response.hmacKeyHex() == null) {
                return null;
            }
            byte[] key = HexFormat.of().parseHex(response.hmacKeyHex());
            cache.put(cacheKey, new CacheEntry<>(key, System.currentTimeMillis() + CACHE_TTL_MS));
            return key;
        } catch (Exception e) {
            log.error("Error fetching HMAC key for device {}: {}", deviceId, e.getMessage());
            authErrorCounter.increment();
            return null;
        }
    }

    @Override
    public void recordFirmwareVersion(String deviceId, String firmwareVersion) {
        try {
            restClient.put()
                    .uri("/internal/devices/{deviceId}/firmware-version", deviceId)
                    .body(new FirmwareVersionUpdate(firmwareVersion))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.debug("Failed to record firmware version for {}: {}", deviceId, e.getMessage());
        }
    }

    private record DeviceAuthResponse(String status, String tenantId) {}
    private record HmacKeyResponse(String hmacKeyHex, int version) {}
    private record FirmwareVersionUpdate(String firmwareVersion) {}
}
