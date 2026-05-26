package com.pyrosense.dashboard.application.usecase;

import com.pyrosense.dashboard.application.port.out.DashboardCachePort;
import com.pyrosense.dashboard.application.port.out.DashboardReadModelPort;
import com.pyrosense.dashboard.domain.model.DashboardOverview;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetDashboardOverviewServiceTest {

    @Mock
    private DashboardReadModelPort readModel;

    @Mock
    private DashboardCachePort cache;

    private GetDashboardOverviewService service;
    private final TenantId tenantId = new TenantId(UUID.randomUUID());
    private final Duration ttl = Duration.ofSeconds(30);

    @BeforeEach
    void setUp() {
        service = new GetDashboardOverviewService(readModel, cache, ttl);
    }

    @Test
    void shouldReturnCachedOverviewWhenPresent() {
        DashboardOverview cached = new DashboardOverview(
                tenantId, 10, 50, 45, 5, 35.0, 2, 5, 3, 1, Instant.now());
        when(cache.get(anyString(), eq(DashboardOverview.class))).thenReturn(Optional.of(cached));

        DashboardOverview result = service.getOverview(tenantId);

        assertThat(result).isEqualTo(cached);
        verifyNoInteractions(readModel);
    }

    @Test
    void shouldQueryReadModelAndCacheWhenNotCached() {
        DashboardOverview overview = new DashboardOverview(
                tenantId, 10, 50, 45, 5, 35.0, 2, 5, 3, 1, Instant.now());
        when(cache.get(anyString(), eq(DashboardOverview.class))).thenReturn(Optional.empty());
        when(readModel.getOverview(tenantId)).thenReturn(overview);

        DashboardOverview result = service.getOverview(tenantId);

        assertThat(result).isEqualTo(overview);
        verify(cache).put(anyString(), eq(overview), eq(ttl));
    }

    @Test
    void shouldUseTenantIdInCacheKey() {
        when(cache.get(contains(tenantId.value().toString()), any())).thenReturn(Optional.empty());
        when(readModel.getOverview(tenantId)).thenReturn(
                new DashboardOverview(tenantId, 0, 0, 0, 0, 0, 0, 0, 0, 0, Instant.now()));

        service.getOverview(tenantId);

        verify(cache).get(contains(tenantId.value().toString()), any());
    }
}
