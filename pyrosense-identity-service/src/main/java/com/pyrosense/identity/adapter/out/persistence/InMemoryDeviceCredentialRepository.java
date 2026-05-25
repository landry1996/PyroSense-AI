package com.pyrosense.identity.adapter.out.persistence;

import com.pyrosense.identity.application.port.out.DeviceCredentialRepository;
import com.pyrosense.identity.domain.model.DeviceCredential;
import com.pyrosense.shared.id.DeviceId;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDeviceCredentialRepository implements DeviceCredentialRepository {

    private final Map<DeviceId, DeviceCredential> store = new ConcurrentHashMap<>();

    @Override
    public DeviceCredential save(DeviceCredential credential) {
        store.put(credential.getDeviceId(), credential);
        return credential;
    }

    @Override
    public Optional<DeviceCredential> findActiveByDeviceId(DeviceId deviceId) {
        return Optional.ofNullable(store.get(deviceId))
                .filter(DeviceCredential::isActive);
    }

    @Override
    public void deleteByDeviceId(DeviceId deviceId) {
        store.remove(deviceId);
    }
}
