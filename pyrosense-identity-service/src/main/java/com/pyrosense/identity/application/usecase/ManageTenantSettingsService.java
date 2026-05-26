package com.pyrosense.identity.application.usecase;

import com.pyrosense.identity.application.port.in.ManageTenantSettingsUseCase;
import com.pyrosense.identity.application.port.out.EmergencyContactRepository;
import com.pyrosense.identity.application.port.out.TenantSettingsRepository;
import com.pyrosense.identity.domain.model.EmergencyContact;
import com.pyrosense.identity.domain.model.TenantSettings;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ManageTenantSettingsService implements ManageTenantSettingsUseCase {

    private final TenantSettingsRepository settingsRepository;
    private final EmergencyContactRepository contactRepository;

    public ManageTenantSettingsService(TenantSettingsRepository settingsRepository,
                                       EmergencyContactRepository contactRepository) {
        this.settingsRepository = settingsRepository;
        this.contactRepository = contactRepository;
    }

    @Override
    public TenantSettings getSettings(TenantId tenantId) {
        return settingsRepository.findByTenantId(tenantId)
                .orElse(new TenantSettings(tenantId, Map.of(), Instant.now()));
    }

    @Override
    public TenantSettings updateSettings(TenantId tenantId, Map<String, Object> settings) {
        TenantSettings existing = settingsRepository.findByTenantId(tenantId)
                .orElse(new TenantSettings(tenantId, Map.of(), Instant.now()));
        existing.update(settings);
        return settingsRepository.save(existing);
    }

    @Override
    public List<EmergencyContact> getEmergencyContacts(TenantId tenantId) {
        return contactRepository.findByTenantId(tenantId);
    }

    @Override
    public EmergencyContact addEmergencyContact(TenantId tenantId, CreateEmergencyContactCommand command) {
        EmergencyContact contact = new EmergencyContact(
                tenantId, command.name(), command.phone(), command.email(), command.role(), command.priority());
        return contactRepository.save(contact);
    }

    @Override
    public EmergencyContact updateEmergencyContact(UUID contactId, UpdateEmergencyContactCommand command) {
        EmergencyContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new NotFoundException("EmergencyContact", contactId));
        contact.update(command.name(), command.phone(), command.email(), command.role(), command.priority());
        return contactRepository.save(contact);
    }

    @Override
    public void deleteEmergencyContact(UUID contactId) {
        contactRepository.findById(contactId)
                .orElseThrow(() -> new NotFoundException("EmergencyContact", contactId));
        contactRepository.deleteById(contactId);
    }
}
