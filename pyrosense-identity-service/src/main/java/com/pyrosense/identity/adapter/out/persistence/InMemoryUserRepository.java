package com.pyrosense.identity.adapter.out.persistence;

import com.pyrosense.identity.application.port.out.UserRepository;
import com.pyrosense.identity.domain.model.User;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("inmemory")
public class InMemoryUserRepository implements UserRepository {

    private final Map<UserId, User> store = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        store.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return store.values().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public List<User> findByTenantId(TenantId tenantId) {
        return store.values().stream()
                .filter(u -> u.getMemberships().stream()
                        .anyMatch(m -> m.getTenantId().equals(tenantId) && m.isActive()))
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return store.values().stream().anyMatch(u -> u.getEmail().equals(email));
    }
}
