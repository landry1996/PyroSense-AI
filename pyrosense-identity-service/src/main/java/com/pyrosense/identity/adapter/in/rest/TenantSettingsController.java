package com.pyrosense.identity.adapter.in.rest;

import com.pyrosense.identity.application.port.in.ManageTenantSettingsUseCase;
import com.pyrosense.identity.application.port.in.ManageTenantSettingsUseCase.CreateEmergencyContactCommand;
import com.pyrosense.identity.application.port.in.ManageTenantSettingsUseCase.UpdateEmergencyContactCommand;
import com.pyrosense.identity.config.Audited;
import com.pyrosense.identity.domain.model.EmergencyContact;
import com.pyrosense.identity.domain.model.TenantSettings;
import com.pyrosense.shared.id.TenantId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}")
public class TenantSettingsController {

    private final ManageTenantSettingsUseCase useCase;

    public TenantSettingsController(ManageTenantSettingsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<SettingsResponse> getSettings(@PathVariable String tenantId) {
        TenantSettings settings = useCase.getSettings(new TenantId(UUID.fromString(tenantId)));
        return ResponseEntity.ok(new SettingsResponse(tenantId, settings.getSettings(), settings.getUpdatedAt()));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Audited(value = "TENANT_SETTINGS_UPDATED", resourceType = "TenantSettings")
    public ResponseEntity<SettingsResponse> updateSettings(@PathVariable String tenantId,
                                                            @RequestBody Map<String, Object> settings) {
        TenantSettings updated = useCase.updateSettings(new TenantId(UUID.fromString(tenantId)), settings);
        return ResponseEntity.ok(new SettingsResponse(tenantId, updated.getSettings(), updated.getUpdatedAt()));
    }

    @GetMapping("/emergency-contacts")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<EmergencyContactResponse>> getEmergencyContacts(@PathVariable String tenantId) {
        List<EmergencyContact> contacts = useCase.getEmergencyContacts(new TenantId(UUID.fromString(tenantId)));
        return ResponseEntity.ok(contacts.stream().map(this::toResponse).toList());
    }

    @PostMapping("/emergency-contacts")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Audited(value = "EMERGENCY_CONTACT_CREATED", resourceType = "EmergencyContact")
    public ResponseEntity<EmergencyContactResponse> addEmergencyContact(
            @PathVariable String tenantId,
            @Valid @RequestBody CreateEmergencyContactRequest request) {
        EmergencyContact contact = useCase.addEmergencyContact(
                new TenantId(UUID.fromString(tenantId)),
                new CreateEmergencyContactCommand(request.name(), request.phone(), request.email(), request.role(), request.priority()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(contact));
    }

    @PutMapping("/emergency-contacts/{contactId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Audited(value = "EMERGENCY_CONTACT_UPDATED", resourceType = "EmergencyContact")
    public ResponseEntity<EmergencyContactResponse> updateEmergencyContact(
            @PathVariable String tenantId,
            @PathVariable String contactId,
            @Valid @RequestBody UpdateEmergencyContactRequest request) {
        EmergencyContact contact = useCase.updateEmergencyContact(
                UUID.fromString(contactId),
                new UpdateEmergencyContactCommand(request.name(), request.phone(), request.email(), request.role(), request.priority()));
        return ResponseEntity.ok(toResponse(contact));
    }

    @DeleteMapping("/emergency-contacts/{contactId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Audited(value = "EMERGENCY_CONTACT_DELETED", resourceType = "EmergencyContact")
    public ResponseEntity<Void> deleteEmergencyContact(
            @PathVariable String tenantId,
            @PathVariable String contactId) {
        useCase.deleteEmergencyContact(UUID.fromString(contactId));
        return ResponseEntity.noContent().build();
    }

    private EmergencyContactResponse toResponse(EmergencyContact c) {
        return new EmergencyContactResponse(c.getId().toString(), c.getTenantId().value().toString(),
                c.getName(), c.getPhone(), c.getEmail(), c.getRole(), c.getPriority(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    record SettingsResponse(String tenantId, Map<String, Object> settings, Instant updatedAt) {}
    record EmergencyContactResponse(String id, String tenantId, String name, String phone,
                                     String email, String role, int priority, Instant createdAt, Instant updatedAt) {}
    record CreateEmergencyContactRequest(@NotBlank String name, @NotBlank String phone, String email,
                                          @NotBlank String role, int priority) {}
    record UpdateEmergencyContactRequest(@NotBlank String name, @NotBlank String phone, String email,
                                          @NotBlank String role, int priority) {}
}
