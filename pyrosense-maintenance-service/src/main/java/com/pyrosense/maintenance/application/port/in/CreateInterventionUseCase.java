package com.pyrosense.maintenance.application.port.in;

import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

public interface CreateInterventionUseCase {

    Intervention createFromAlert(CreateInterventionCommand command);

    record CreateInterventionCommand(
            TenantId tenantId,
            AlertId alertId,
            DeviceId deviceId,
            String severity,
            String alertType,
            String description
    ) {}
}
