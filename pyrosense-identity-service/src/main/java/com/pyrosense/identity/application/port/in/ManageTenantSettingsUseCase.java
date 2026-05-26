package com.pyrosense.identity.application.port.in;

import com.pyrosense.identity.domain.model.EmergencyContact;
import com.pyrosense.identity.domain.model.TenantSettings;
import com.pyrosense.shared.id.TenantId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ManageTenantSettingsUseCase {
    TenantSettings getSettings(TenantId tenantId);
    TenantSettings updateSettings(TenantId tenantId, Map<String, Object> settings);
    List<EmergencyContact> getEmergencyContacts(TenantId tenantId);
    EmergencyContact addEmergencyContact(TenantId tenantId, CreateEmergencyContactCommand command);
    EmergencyContact updateEmergencyContact(UUID contactId, UpdateEmergencyContactCommand command);
    void deleteEmergencyContact(UUID contactId);

    record CreateEmergencyContactCommand(String name, String phone, String email, String role, int priority) {}
    record UpdateEmergencyContactCommand(String name, String phone, String email, String role, int priority) {}
}
