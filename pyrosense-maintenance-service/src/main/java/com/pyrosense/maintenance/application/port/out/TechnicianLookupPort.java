package com.pyrosense.maintenance.application.port.out;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

public interface TechnicianLookupPort {

    boolean exists(UserId technicianId, TenantId tenantId);
}
