package com.pyrosense.maintenance.application.port.out;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.util.UUID;

public interface RiskScoreReevaluationPublisherPort {

    void requestReevaluation(TenantId tenantId, DeviceId deviceId, UUID interventionId);
}
