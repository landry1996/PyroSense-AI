package com.pyrosense.device.adapter.out.persistence.repository;

import com.pyrosense.device.adapter.out.persistence.entity.DeviceJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceJpaRepository extends JpaRepository<DeviceJpaEntity, UUID> {

    Optional<DeviceJpaEntity> findBySerialNumber(String serialNumber);

    Page<DeviceJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    boolean existsBySerialNumber(String serialNumber);
}
