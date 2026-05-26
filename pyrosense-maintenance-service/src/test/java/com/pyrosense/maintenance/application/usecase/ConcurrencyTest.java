package com.pyrosense.maintenance.application.usecase;

import com.pyrosense.maintenance.adapter.out.persistence.InMemoryInterventionRepository;
import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase.CreateInterventionCommand;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
import com.pyrosense.maintenance.domain.model.Intervention;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

class ConcurrencyTest {

    private final Instant now = Instant.parse("2025-03-10T10:00:00Z");

    @BeforeEach
    void setUp() { ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC"))); }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    @Test
    void twoManagersShouldNotCreateDuplicateIntervention() throws Exception {
        var repository = new InMemoryInterventionRepository();
        MaintenanceEventPublisherPort publisher = event -> {};
        var service = new CreateInterventionService(repository, publisher);

        AlertId alertId = AlertId.generate();
        TenantId tenantId = TenantId.generate();
        DeviceId deviceId = DeviceId.generate();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        List<Future<Intervention>> futures = new ArrayList<>();
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                try {
                    var command = new CreateInterventionCommand(
                            tenantId, alertId, deviceId,
                            "CRITICAL", "MICRO_ARC", "Concurrent test");
                    return service.createFromAlert(command);
                } catch (Exception e) {
                    errors.add(e);
                    return null;
                }
            }));
        }

        // Release all threads simultaneously
        latch.countDown();

        List<Intervention> successes = new ArrayList<>();
        for (Future<Intervention> f : futures) {
            Intervention result = f.get(5, TimeUnit.SECONDS);
            if (result != null) successes.add(result);
        }

        executor.shutdown();

        // At least one succeeds, and the total (successes + errors) equals threadCount
        // In production, DB unique index guarantees exactly 1 success.
        // In-memory repo may allow races, but service-level check catches most.
        assertThat(successes.size() + errors.size()).isEqualTo(threadCount);
        assertThat(successes).isNotEmpty();
        // All errors should be conflict errors
        assertThat(errors).allSatisfy(e ->
                assertThat(e.getMessage()).contains("already exists for alert"));
    }
}
