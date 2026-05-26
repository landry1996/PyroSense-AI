package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.out.DashboardCachePort;
import com.pyrosense.dashboard.application.port.out.DashboardReadModelPort;
import com.pyrosense.dashboard.domain.model.RiskyBuilding;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetRiskyBuildingsServiceTest {

    @Mock
    private DashboardReadModelPort readModel;

    @Mock
    private DashboardCachePort cache;

    private GetRiskyBuildingsService service;
    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        service = new GetRiskyBuildingsService(readModel, cache, Duration.ofSeconds(60));
    }

    @Test
    void shouldReturnRiskyBuildingsFromReadModel() {
        List<RiskyBuilding> buildings = List.of(
                new RiskyBuilding("b1", "Building A", "1 Rue Test", 85.0, "CRITICAL", 3, 2, "CRITICAL", Instant.now()));
        when(cache.get(anyString(), any())).thenReturn(Optional.empty());
        when(readModel.getRiskyBuildings(tenantId, 10)).thenReturn(buildings);

        List<RiskyBuilding> result = service.getRiskyBuildings(tenantId, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Building A");
    }

    @Test
    void shouldCacheResults() {
        when(cache.get(anyString(), any())).thenReturn(Optional.empty());
        when(readModel.getRiskyBuildings(any(), anyInt())).thenReturn(List.of());

        service.getRiskyBuildings(tenantId, 5);

        verify(cache).put(anyString(), any(), eq(Duration.ofSeconds(60)));
    }
}
