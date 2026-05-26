package com.pyrosense.notification.domain;

import com.pyrosense.notification.application.port.in.SendNotificationUseCase.DispatchAlertNotificationCommand;
import com.pyrosense.notification.application.port.out.*;
import com.pyrosense.notification.application.usecase.NotificationDispatcher;
import com.pyrosense.notification.application.usecase.SendNotificationService;
import com.pyrosense.notification.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicationAntiSpamTest {

    private InMemoryNotificationRepo repository;
    private SingleRecipientResolver recipientResolver;
    private WindowedDeduplication deduplication;
    private SendNotificationService service;

    private static final TenantId TENANT = new TenantId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepo();
        recipientResolver = new SingleRecipientResolver();
        deduplication = new WindowedDeduplication(Duration.ofMinutes(30));
        var dispatcher = new NotificationDispatcher(
                (to, s, b) -> {}, (p, m) -> {}, (t, ti, b) -> {}, (u, payload) -> {});
        service = new SendNotificationService(repository, recipientResolver, deduplication, dispatcher);
    }

    @Test
    void shouldNotResendSameAlertWithinWindow() {
        var command = createCommand("alert-1", "device-1", AlertSeverity.CRITICAL);

        List<Notification> first = service.dispatchForAlert(command);
        assertFalse(first.isEmpty());

        List<Notification> second = service.dispatchForAlert(command);
        assertTrue(second.isEmpty());
    }

    @Test
    void shouldAllowDifferentAlertsForSameDevice() {
        var cmd1 = createCommand("alert-1", "device-1", AlertSeverity.WARNING);
        var cmd2 = createCommand("alert-2", "device-1", AlertSeverity.WARNING);

        List<Notification> first = service.dispatchForAlert(cmd1);
        List<Notification> second = service.dispatchForAlert(cmd2);

        assertFalse(first.isEmpty());
        assertFalse(second.isEmpty());
    }

    @Test
    void shouldAllowSameAlertAfterWindowExpires() {
        var command = createCommand("alert-1", "device-1", AlertSeverity.WARNING);

        List<Notification> first = service.dispatchForAlert(command);
        assertFalse(first.isEmpty());

        deduplication.expireAll();

        List<Notification> second = service.dispatchForAlert(command);
        assertFalse(second.isEmpty());
    }

    @Test
    void shouldAllowEscalationAfterInitialAlert() {
        var initial = createCommand("alert-1", "device-1", AlertSeverity.WARNING);
        service.dispatchForAlert(initial);

        var escalation = createCommand("alert-1", "device-1", AlertSeverity.CRITICAL);
        List<Notification> escalated = service.dispatchForAlert(escalation);

        assertFalse(escalated.isEmpty());
    }

    @Test
    void shouldDeduplicatePerRecipientAndChannel() {
        var command = createCommand("alert-1", "device-1", AlertSeverity.CRITICAL);

        service.dispatchForAlert(command);
        int total = repository.findAll().size();

        service.dispatchForAlert(command);
        assertEquals(total, repository.findAll().size());
    }

    @Test
    void criticalAlertShouldSendToAllChannels() {
        var command = createCommand("alert-new", "device-1", AlertSeverity.CRITICAL);
        List<Notification> result = service.dispatchForAlert(command);

        Set<NotificationChannel> channels = new HashSet<>();
        result.forEach(n -> channels.add(n.getChannel()));
        assertTrue(channels.contains(NotificationChannel.SMS));
        assertTrue(channels.contains(NotificationChannel.PUSH));
        assertTrue(channels.contains(NotificationChannel.EMAIL));
        assertTrue(channels.contains(NotificationChannel.DASHBOARD));
    }

    private DispatchAlertNotificationCommand createCommand(String alertId, String deviceId, AlertSeverity severity) {
        return new DispatchAlertNotificationCommand(TENANT, alertId, deviceId, severity, "MICRO_ARC", "2025-01-15T10:00:00Z");
    }

    static class InMemoryNotificationRepo implements NotificationRepositoryPort {
        private final List<Notification> store = new ArrayList<>();
        @Override public Notification save(Notification n) { store.removeIf(x -> x.getId().equals(n.getId())); store.add(n); return n; }
        @Override public Optional<Notification> findById(UUID id) { return store.stream().filter(n -> n.getId().equals(id)).findFirst(); }
        @Override public List<Notification> findByRecipientId(UserId r) { return store.stream().filter(n -> n.getRecipientId().equals(r)).toList(); }
        @Override public List<Notification> findByTenantId(TenantId t) { return store.stream().filter(n -> n.getTenantId().equals(t)).toList(); }
        @Override public List<Notification> findByStatus(NotificationStatus s) { return store.stream().filter(n -> n.getStatus() == s).toList(); }
        @Override public List<Notification> findPendingRetries() { return store.stream().filter(Notification::shouldRetryNow).toList(); }
        @Override public long countByTenantIdAndStatus(TenantId t, NotificationStatus s) { return store.stream().filter(n -> n.getTenantId().equals(t) && n.getStatus() == s).count(); }
        List<Notification> findAll() { return new ArrayList<>(store); }
    }

    static class SingleRecipientResolver implements RecipientResolverPort {
        private final UserId fixedUser = new UserId(UUID.randomUUID());
        @Override
        public List<Recipient> resolve(TenantId tenantId, List<RecipientType> types) {
            return List.of(new Recipient(fixedUser, tenantId, RecipientType.PROPERTY_MANAGER,
                    "pm@test.com", "+33600000000", "push-token-pm", true, true, true));
        }
    }

    static class WindowedDeduplication implements DeduplicationPort {
        private final Map<String, Instant> sent = new ConcurrentHashMap<>();
        private final Duration window;

        WindowedDeduplication(Duration window) { this.window = window; }

        @Override public boolean isDuplicate(DeduplicationKey key) {
            Instant at = sent.get(key.toKeyString());
            if (at == null) return false;
            return Instant.now().isBefore(at.plus(window));
        }
        @Override public void markSent(DeduplicationKey key) { sent.put(key.toKeyString(), Instant.now()); }
        void expireAll() { sent.clear(); }
    }
}
