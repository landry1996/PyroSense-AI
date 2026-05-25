package com.pyrosense.identity.application.port.in;

import com.pyrosense.identity.domain.model.Tenant;
import com.pyrosense.shared.id.TenantId;

import java.util.Optional;

public interface ManageTenantUseCase {

    Tenant create(CreateTenantCommand command);

    Optional<Tenant> findById(TenantId tenantId);

    void deactivate(TenantId tenantId);

    record CreateTenantCommand(String name, String slug) {}
}
