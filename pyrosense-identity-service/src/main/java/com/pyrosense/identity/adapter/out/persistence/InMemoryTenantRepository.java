package com.pyrosense.identity.adapter.out.persistence;

import com.pyrosense.identity.application.port.out.TenantRepository;
import com.pyrosense.identity.domain.model.Tenant;
import com.pyrosense.shared.id.TenantId;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTenantRepository implements TenantRepository {

    private final Map<TenantId, Tenant> store = new ConcurrentHashMap<>();

    @Override
    public Tenant save(Tenant tenant) {
        store.put(tenant.getId(), tenant);
        return tenant;
    }

    @Override
    public Optional<Tenant> findById(TenantId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return store.values().stream()
                .filter(t -> t.getSlug().equals(slug))
                .findFirst();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return store.values().stream().anyMatch(t -> t.getSlug().equals(slug));
    }
}
