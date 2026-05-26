package com.pyrosense.maintenance.domain.model;

import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class InterventionRecommendationTest {

    private final Instant now = Instant.parse("2025-03-10T10:00:00Z");

    @BeforeEach
    void setUp() { ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC"))); }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    private InterventionRecommendation createRecommendation() {
        return new InterventionRecommendation(
                UUID.randomUUID(), TenantId.generate(), AlertId.generate(), DeviceId.generate(),
                InterventionType.PREVENTIVE, InterventionPriority.HIGH,
                "Warning alert detected", Duration.ofHours(24));
    }

    @Test
    void shouldCreateWithPendingStatus() {
        var rec = createRecommendation();

        assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.PENDING);
        assertThat(rec.getSlaExpiresAt()).isEqualTo(now.plus(Duration.ofHours(24)));
        assertThat(rec.getDecidedAt()).isNull();
    }

    @Test
    void shouldAccept() {
        var rec = createRecommendation();
        UUID interventionId = UUID.randomUUID();

        rec.accept(interventionId);

        assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.ACCEPTED);
        assertThat(rec.getAcceptedInterventionId()).isEqualTo(interventionId);
        assertThat(rec.getDecidedAt()).isEqualTo(now);
    }

    @Test
    void shouldReject() {
        var rec = createRecommendation();

        rec.reject("Not needed at this time");

        assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.REJECTED);
        assertThat(rec.getRejectionReason()).isEqualTo("Not needed at this time");
    }

    @Test
    void shouldNotAcceptTwice() {
        var rec = createRecommendation();
        rec.accept(UUID.randomUUID());

        assertThatThrownBy(() -> rec.accept(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotRejectAfterAccepted() {
        var rec = createRecommendation();
        rec.accept(UUID.randomUUID());

        assertThatThrownBy(() -> rec.reject("too late"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectBlankReason() {
        var rec = createRecommendation();

        assertThatThrownBy(() -> rec.reject(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldExpire() {
        var rec = createRecommendation();

        rec.expire();

        assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.EXPIRED);
    }

    @Test
    void shouldDetectSlaBreached() {
        ClockProvider.setClock(Clock.fixed(now.plus(Duration.ofHours(25)), ZoneId.of("UTC")));
        var rec = new InterventionRecommendation(
                UUID.randomUUID(), TenantId.generate(), AlertId.generate(), DeviceId.generate(),
                InterventionType.PREVENTIVE, InterventionPriority.HIGH,
                "Warning alert", Duration.ofHours(24));

        // Created at now+25h, sla expires at now+49h, so not breached yet
        assertThat(rec.isSlaBreached()).isFalse();

        // Reset to original time — rec was created at now+25h with 24h SLA, so expires at now+49h
        ClockProvider.setClock(Clock.fixed(now.plus(Duration.ofHours(50)), ZoneId.of("UTC")));
        assertThat(rec.isSlaBreached()).isTrue();
    }
}
