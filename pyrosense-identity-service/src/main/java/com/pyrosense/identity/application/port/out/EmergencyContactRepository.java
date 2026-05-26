package com.pyrosense.identity.application.port.out;

import com.pyrosense.identity.domain.model.EmergencyContact;
import com.pyrosense.shared.id.TenantId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyContactRepository {
    List<EmergencyContact> findByTenantId(TenantId tenantId);
    Optional<EmergencyContact> findById(UUID id);
    EmergencyContact save(EmergencyContact contact);
    void deleteById(UUID id);
}
