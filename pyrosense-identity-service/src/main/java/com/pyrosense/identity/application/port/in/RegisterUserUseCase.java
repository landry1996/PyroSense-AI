package com.pyrosense.identity.application.port.in;

import com.pyrosense.identity.domain.model.User;
import com.pyrosense.shared.id.TenantId;

import java.util.Set;

public interface RegisterUserUseCase {

    User register(RegisterUserCommand command);

    record RegisterUserCommand(
            String email,
            String fullName,
            TenantId tenantId,
            Set<String> roles
    ) {}
}
