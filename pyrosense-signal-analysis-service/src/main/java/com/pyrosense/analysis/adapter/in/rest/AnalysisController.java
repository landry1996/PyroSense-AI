package com.pyrosense.analysis.adapter.in.rest;

import com.pyrosense.analysis.application.port.out.AnomalyRepositoryPort;
import com.pyrosense.analysis.application.port.out.BaselineProfileRepositoryPort;
import com.pyrosense.analysis.domain.model.BaselineProfile;
import com.pyrosense.analysis.domain.model.SignalAnomaly;
import com.pyrosense.shared.id.DeviceId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','PROPERTY_MANAGER','ELECTRICIAN','OPERATOR')")
public class AnalysisController {

    private final AnomalyRepositoryPort anomalyRepository;
    private final BaselineProfileRepositoryPort baselineRepository;

    public AnalysisController(AnomalyRepositoryPort anomalyRepository,
                              BaselineProfileRepositoryPort baselineRepository) {
        this.anomalyRepository = anomalyRepository;
        this.baselineRepository = baselineRepository;
    }

    @GetMapping("/anomalies/{deviceId}")
    public ResponseEntity<AnomalyResponse> getAnomalies(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "24") int hoursBack) {
        DeviceId id = DeviceId.from(deviceId);
        Instant from = Instant.now().minus(Duration.ofHours(hoursBack));
        Instant to = Instant.now();

        List<SignalAnomaly> anomalies = anomalyRepository.findByDeviceId(id, from, to);
        long total = anomalyRepository.countByDeviceId(id, from, to);

        return ResponseEntity.ok(new AnomalyResponse(deviceId, anomalies, total, hoursBack));
    }

    @GetMapping("/baseline/{deviceId}")
    public ResponseEntity<BaselineResponse> getBaseline(@PathVariable String deviceId) {
        DeviceId id = DeviceId.from(deviceId);
        return baselineRepository.findByDeviceId(id)
                .map(p -> ResponseEntity.ok(toBaselineResponse(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health/status")
    public ResponseEntity<ServiceStatus> serviceStatus() {
        return ResponseEntity.ok(new ServiceStatus("signal-analysis", "UP", Instant.now()));
    }

    private BaselineResponse toBaselineResponse(BaselineProfile profile) {
        return new BaselineResponse(
                profile.deviceId().value().toString(),
                profile.sampleCount(),
                profile.isReady(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }

    record AnomalyResponse(String deviceId, List<SignalAnomaly> anomalies, long totalCount, int hoursBack) {}
    record BaselineResponse(String deviceId, int sampleCount, boolean ready, Instant createdAt, Instant updatedAt) {}
    record ServiceStatus(String service, String status, Instant timestamp) {}
}
