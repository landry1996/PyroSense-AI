package com.pyrosense.maintenance.adapter.out.stub;

import com.pyrosense.maintenance.application.port.out.TechnicianLookupPort;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import org.springframework.stereotype.Component;

@Component
public class StubTechnicianLookupAdapter implements TechnicianLookupPort {

    @Override
    public boolean exists(UserId technicianId, TenantId tenantId) {
        return true;
    }
}
