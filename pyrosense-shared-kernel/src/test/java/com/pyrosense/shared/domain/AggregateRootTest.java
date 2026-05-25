package com.pyrosense.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AggregateRootTest {

    @Test
    void shouldStartWithNoEvents() {
        var aggregate = new TestAggregate("id-1");
        assertThat(aggregate.getDomainEvents()).isEmpty();
        assertThat(aggregate.hasEvents()).isFalse();
    }

    @Test
    void shouldRegisterDomainEvents() {
        var aggregate = new TestAggregate("id-1");
        var event = new TestEvent(UUID.randomUUID(), Instant.now());

        aggregate.doSomething(event);

        assertThat(aggregate.getDomainEvents()).hasSize(1);
        assertThat(aggregate.getDomainEvents().get(0)).isEqualTo(event);
        assertThat(aggregate.hasEvents()).isTrue();
    }

    @Test
    void shouldClearDomainEvents() {
        var aggregate = new TestAggregate("id-1");
        aggregate.doSomething(new TestEvent(UUID.randomUUID(), Instant.now()));
        aggregate.doSomething(new TestEvent(UUID.randomUUID(), Instant.now()));

        aggregate.clearDomainEvents();

        assertThat(aggregate.getDomainEvents()).isEmpty();
        assertThat(aggregate.hasEvents()).isFalse();
    }

    @Test
    void shouldReturnUnmodifiableEventList() {
        var aggregate = new TestAggregate("id-1");
        aggregate.doSomething(new TestEvent(UUID.randomUUID(), Instant.now()));

        var events = aggregate.getDomainEvents();

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> events.add(new TestEvent(UUID.randomUUID(), Instant.now()))
        );
    }

    static class TestAggregate extends AggregateRoot<String> {
        private final String id;

        TestAggregate(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        void doSomething(DomainEvent event) {
            registerEvent(event);
        }
    }

    record TestEvent(UUID eventId, Instant occurredAt) implements DomainEvent {
        @Override
        public String eventType() {
            return "test.event";
        }
    }
}
