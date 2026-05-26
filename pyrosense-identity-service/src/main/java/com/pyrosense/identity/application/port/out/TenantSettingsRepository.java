package com.pyrosense.identity.application.port.out;

import com.pyrosense.identity.domain.model.TenantSettings;
import com.pyrosense.shared.id.TenantId;
import java.util.Optional;

public interface TenantSettingsRepository {
    Optional<TenantSettings> findByTenantId(TenantId tenantId);
    TenantSettings save(TenantSettings settings);
}
