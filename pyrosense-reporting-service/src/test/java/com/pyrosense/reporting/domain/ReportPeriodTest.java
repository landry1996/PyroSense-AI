package com.pyrosense.reporting.domain;

import com.pyrosense.reporting.domain.model.ReportPeriod;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReportPeriodTest {

    @Test
    void shouldCreateValidPeriod() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = Instant.parse("2025-01-31T23:59:59Z");
        ReportPeriod period = new ReportPeriod(start, end);

        assertEquals(start, period.start());
        assertEquals(end, period.end());
    }

    @Test
    void shouldRejectNullStart() {
        assertThrows(NullPointerException.class,
                () -> new ReportPeriod(null, Instant.now()));
    }

    @Test
    void shouldRejectNullEnd() {
        assertThrows(NullPointerException.class,
                () -> new ReportPeriod(Instant.now(), null));
    }

    @Test
    void shouldRejectEndBeforeStart() {
        Instant start = Instant.parse("2025-02-01T00:00:00Z");
        Instant end = Instant.parse("2025-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> new ReportPeriod(start, end));
    }
}
