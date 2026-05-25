package com.pyrosense.alerting.application.port.in;

import com.pyrosense.alerting.domain.model.Alert;
import com.pyrosense.alerting.domain.model.AlertType;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.valueobject.AlertSeverity;

public interface CreateAlertUseCase {

    Alert create(CreateAlertCommand command);

    record CreateAlertCommand(
            TenantId tenantId,
            DeviceId deviceId,
            AlertType type,
            AlertSeverity severity,
            String title,
            String description
    ) {}
}
