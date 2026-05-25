package com.pyrosense.notification.application.port.out;

import com.pyrosense.notification.domain.model.Recipient;
import com.pyrosense.notification.domain.model.RecipientType;
import com.pyrosense.shared.id.TenantId;

import java.util.List;

public interface RecipientResolverPort {
    List<Recipient> resolve(TenantId tenantId, List<RecipientType> types);
}
