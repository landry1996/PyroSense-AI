package com.pyrosense.shared.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationEventTest {

    @Test
    void builder_shouldCreateEventWithAllFields() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        IntegrationEvent event = IntegrationEvent.builder()
                .eventId(eventId)
                .eventType("alerting.alert.created")
                .version(1)
                .occurredAt(now)
                .sourceService("pyrosense-alerting-service")
                .tenantId("tenant-123")
                .correlationId("corr-456")
                .causationId("cause-789")
                .payload("{\"alertId\":\"abc\"}")
                .build();

        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.eventType()).isEqualTo("alerting.alert.created");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.occurredAt()).isEqualTo(now);
        assertThat(event.sourceService()).isEqualTo("pyrosense-alerting-service");
        assertThat(event.tenantId()).isEqualTo("tenant-123");
        assertThat(event.correlationId()).isEqualTo("corr-456");
        assertThat(event.causationId()).isEqualTo("cause-789");
        assertThat(event.payload()).isEqualTo("{\"alertId\":\"abc\"}");
    }

    @Test
    void builder_shouldDefaultCorrelationIdToEventId() {
        UUID eventId = UUID.randomUUID();

        IntegrationEvent event = IntegrationEvent.builder()
                .eventId(eventId)
                .eventType("test.event")
                .sourceService("test-service")
                .payload("{}")
                .build();

        assertThat(event.correlationId()).isEqualTo(eventId.toString());
        assertThat(event.causationId()).isEqualTo(eventId.toString());
    }

    @Test
    void builder_shouldDefaultTenantIdToUnknown() {
        IntegrationEvent event = IntegrationEvent.builder()
                .eventType("test.event")
                .sourceService("test-service")
                .payload("{}")
                .build();

        assertThat(event.tenantId()).isEqualTo("unknown");
    }

    @Test
    void of_shouldCreateEventWithStaticFactory() {
        IntegrationEvent event = IntegrationEvent.of(
                "test.event", "test-service", "{\"key\":\"val\"}");

        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo("test.event");
        assertThat(event.sourceService()).isEqualTo("test-service");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.payload()).isEqualTo("{\"key\":\"val\"}");
    }

    @SuppressWarnings("deprecation")
    @Test
    void deprecatedConstructor_shouldMaintainBackwardCompatibility() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        IntegrationEvent event = new IntegrationEvent(
                eventId, "test.event", now, "source-service", "{\"data\":1}");

        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.eventType()).isEqualTo("test.event");
        assertThat(event.occurredAt()).isEqualTo(now);
        assertThat(event.sourceService()).isEqualTo("source-service");
        assertThat(event.payload()).isEqualTo("{\"data\":1}");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.correlationId()).isEqualTo(eventId.toString());
    }

    @Test
    void shouldRejectNullEventType() {
        assertThatThrownBy(() -> IntegrationEvent.builder()
                .sourceService("test-service")
                .payload("{}")
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullPayload() {
        assertThatThrownBy(() -> IntegrationEvent.builder()
                .eventType("test.event")
                .sourceService("test-service")
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullSourceService() {
        assertThatThrownBy(() -> IntegrationEvent.builder()
                .eventType("test.event")
                .payload("{}")
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void eventMetadata_shouldExtractFromEvent() {
        IntegrationEvent event = IntegrationEvent.builder()
                .eventType("test.event")
                .sourceService("test-service")
                .tenantId("t-1")
                .correlationId("c-1")
                .payload("{}")
                .build();

        EventMetadata metadata = EventMetadata.from(event);

        assertThat(metadata.correlationId()).isEqualTo("c-1");
        assertThat(metadata.causationId()).isEqualTo(event.eventId().toString());
        assertThat(metadata.tenantId()).isEqualTo("t-1");
        assertThat(metadata.sourceService()).isEqualTo("test-service");
    }
}
