package com.pyrosense.device.adapter.out.persistence.repository;

import com.pyrosense.device.adapter.out.persistence.entity.DeviceJpaEntity;
import com.pyrosense.device.domain.model.DeviceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceJpaRepository extends JpaRepository<DeviceJpaEntity, UUID> {

    Optional<DeviceJpaEntity> findBySerialNumber(String serialNumber);

    Page<DeviceJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<DeviceJpaEntity> findByBuildingIdAndTenantId(UUID buildingId, UUID tenantId, Pageable pageable);

    boolean existsBySerialNumber(String serialNumber);

    @Query("SELECT d.status, COUNT(d) FROM DeviceJpaEntity d WHERE d.tenantId = :tenantId GROUP BY d.status")
    List<Object[]> countGroupedByStatus(@Param("tenantId") UUID tenantId);

    @Query("SELECT DISTINCT d.buildingId FROM DeviceJpaEntity d WHERE d.tenantId = :tenantId AND d.buildingId IS NOT NULL")
    List<UUID> findDistinctBuildingIdsByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT d.status, COUNT(d) FROM DeviceJpaEntity d WHERE d.buildingId = :buildingId AND d.tenantId = :tenantId GROUP BY d.status")
    List<Object[]> countGroupedByStatusAndBuildingId(@Param("buildingId") UUID buildingId, @Param("tenantId") UUID tenantId);
}
