package com.pyrosense.identity.application.port.out;

import com.pyrosense.identity.domain.model.User;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(String email);

    List<User> findByTenantId(TenantId tenantId);

    boolean existsByEmail(String email);
}
