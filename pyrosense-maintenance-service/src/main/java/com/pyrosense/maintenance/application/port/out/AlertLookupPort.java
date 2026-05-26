package com.pyrosense.maintenance.application.port.out;

import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.TenantId;

import java.util.Optional;

public interface AlertLookupPort {

    record AlertInfo(AlertId alertId, TenantId tenantId, String severity, String type, String deviceId) {}

    Optional<AlertInfo> findById(AlertId alertId);
}
