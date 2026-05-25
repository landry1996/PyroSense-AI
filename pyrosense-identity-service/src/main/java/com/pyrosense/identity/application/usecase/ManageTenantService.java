package com.pyrosense.identity.application.usecase;

import com.pyrosense.identity.application.port.in.ManageTenantUseCase;
import com.pyrosense.identity.application.port.out.TenantRepository;
import com.pyrosense.identity.domain.model.Tenant;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.ErrorCode;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.TenantId;

import java.util.Optional;

public class ManageTenantService implements ManageTenantUseCase {

    private final TenantRepository tenantRepository;

    public ManageTenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public Tenant create(CreateTenantCommand command) {
        if (tenantRepository.existsBySlug(command.slug())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Tenant slug already exists: " + command.slug());
        }
        Tenant tenant = new Tenant(TenantId.generate(), command.name(), command.slug());
        return tenantRepository.save(tenant);
    }

    @Override
    public Optional<Tenant> findById(TenantId tenantId) {
        return tenantRepository.findById(tenantId);
    }

    @Override
    public void deactivate(TenantId tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant", tenantId.value()));
        tenant.deactivate();
        tenantRepository.save(tenant);
    }
}
