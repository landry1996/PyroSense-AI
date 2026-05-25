package com.pyrosense.ingestion.application.port.out;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.List;

public interface RejectionRepositoryPort {

    void saveRejection(TenantId tenantId, DeviceId deviceId, String reason,
                       String source, String payloadHash, List<String> violations);
}
