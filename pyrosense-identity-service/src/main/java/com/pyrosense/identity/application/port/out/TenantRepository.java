package com.pyrosense.identity.application.port.out;

import com.pyrosense.identity.domain.model.Tenant;
import com.pyrosense.shared.id.TenantId;

import java.util.Optional;

public interface TenantRepository {

    Tenant save(Tenant tenant);

    Optional<Tenant> findById(TenantId id);

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
