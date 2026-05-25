package com.pyrosense.identity.application.usecase;

import com.pyrosense.identity.application.port.in.RegisterUserUseCase;
import com.pyrosense.identity.application.port.out.AuditLogRepository;
import com.pyrosense.identity.application.port.out.TenantRepository;
import com.pyrosense.identity.application.port.out.UserRepository;
import com.pyrosense.identity.domain.model.Role;
import com.pyrosense.identity.domain.model.User;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.ErrorCode;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.security.AuditEntry;

import java.util.Set;
import java.util.stream.Collectors;

public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final AuditLogRepository auditLog;

    public RegisterUserService(UserRepository userRepository, TenantRepository tenantRepository,
                                AuditLogRepository auditLog) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.auditLog = auditLog;
    }

    @Override
    public User register(RegisterUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Email already registered: " + command.email());
        }

        tenantRepository.findById(command.tenantId())
                .orElseThrow(() -> new NotFoundException("Tenant", command.tenantId().value()));

        Set<Role> roles = command.roles().stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        User user = new User(UserId.generate(), command.email(), command.fullName());
        user.addMembership(command.tenantId(), roles);
        User saved = userRepository.save(user);

        auditLog.save(AuditEntry.create("USER_REGISTERED", "User", saved.getId().value().toString(),
                saved.getId(), command.tenantId(), null, null,
                "Registered with roles: " + command.roles()));

        return saved;
    }
}
