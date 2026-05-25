package com.pyrosense.device.adapter.out.persistence;

import com.pyrosense.device.adapter.out.persistence.mapper.DevicePersistenceMapper;
import com.pyrosense.device.adapter.out.persistence.repository.DeviceJpaRepository;
import com.pyrosense.device.application.port.out.DeviceRepositoryPort;
import com.pyrosense.device.domain.model.Device;
import com.pyrosense.device.domain.model.DeviceStatus;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.pagination.Page;
import com.pyrosense.shared.pagination.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class DevicePersistenceAdapter implements DeviceRepositoryPort {

    private final DeviceJpaRepository jpaRepository;

    public DevicePersistenceAdapter(DeviceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Device save(Device device) {
        var entity = DevicePersistenceMapper.toEntity(device);
        var saved = jpaRepository.save(entity);
        return DevicePersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Device> findById(DeviceId id) {
        return jpaRepository.findById(id.value())
                .map(DevicePersistenceMapper::toDomain);
    }

    @Override
    public Optional<Device> findBySerialNumber(String serialNumber) {
        return jpaRepository.findBySerialNumber(serialNumber)
                .map(DevicePersistenceMapper::toDomain);
    }

    @Override
    public Page<Device> findByTenantId(TenantId tenantId, PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.page(),
                pageRequest.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        var jpaPage = jpaRepository.findByTenantId(tenantId.value(), pageable);
        var devices = jpaPage.getContent().stream()
                .map(DevicePersistenceMapper::toDomain)
                .toList();
        return Page.of(devices, jpaPage.getNumber(), jpaPage.getSize(), jpaPage.getTotalElements());
    }

    @Override
    public Page<Device> findByBuildingId(BuildingId buildingId, TenantId tenantId, PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.page(),
                pageRequest.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        var jpaPage = jpaRepository.findByBuildingIdAndTenantId(buildingId.value(), tenantId.value(), pageable);
        var devices = jpaPage.getContent().stream()
                .map(DevicePersistenceMapper::toDomain)
                .toList();
        return Page.of(devices, jpaPage.getNumber(), jpaPage.getSize(), jpaPage.getTotalElements());
    }

    @Override
    public boolean existsBySerialNumber(String serialNumber) {
        return jpaRepository.existsBySerialNumber(serialNumber);
    }

    @Override
    public Map<DeviceStatus, Integer> countByTenantAndStatus(TenantId tenantId) {
        var results = jpaRepository.countGroupedByStatus(tenantId.value());
        Map<DeviceStatus, Integer> counts = new EnumMap<>(DeviceStatus.class);
        for (Object[] row : results) {
            DeviceStatus status = (DeviceStatus) row[0];
            int count = ((Number) row[1]).intValue();
            counts.put(status, count);
        }
        return counts;
    }

    @Override
    public List<BuildingId> findDistinctBuildingIdsByTenant(TenantId tenantId) {
        return jpaRepository.findDistinctBuildingIdsByTenantId(tenantId.value()).stream()
                .map(uuid -> new BuildingId(uuid))
                .toList();
    }

    @Override
    public Map<DeviceStatus, Integer> countByBuildingIdAndStatus(BuildingId buildingId, TenantId tenantId) {
        var results = jpaRepository.countGroupedByStatusAndBuildingId(buildingId.value(), tenantId.value());
        Map<DeviceStatus, Integer> counts = new EnumMap<>(DeviceStatus.class);
        for (Object[] row : results) {
            DeviceStatus status = (DeviceStatus) row[0];
            int count = ((Number) row[1]).intValue();
            counts.put(status, count);
        }
        return counts;
    }
}
