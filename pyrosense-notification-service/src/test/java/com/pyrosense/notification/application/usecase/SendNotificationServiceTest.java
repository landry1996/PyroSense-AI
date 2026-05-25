package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.SendNotificationUseCase.DispatchAlertNotificationCommand;
import com.pyrosense.notification.application.port.out.*;
import com.pyrosense.notification.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SendNotificationServiceTest {

    private InMemoryNotificationRepo repository;
    private FakeRecipientResolver recipientResolver;
    private FakeDeduplication deduplication;
    private FakeDispatcher dispatcher;
    private SendNotificationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepo();
        recipientResolver = new FakeRecipientResolver();
        deduplication = new FakeDeduplication();
        dispatcher = new FakeDispatcher();
        var notifDispatcher = new NotificationDispatcher(
                new NoOpEmail(), new NoOpSms(), new NoOpPush(), new NoOpWebhook());
        service = new SendNotificationService(repository, recipientResolver, deduplication, notifDispatcher);
    }

    @Test
    void shouldDispatchDashboardOnlyForInfo() {
        var command = createCommand(AlertSeverity.INFO);
        List<Notification> result = service.dispatchForAlert(command);

        assertTrue(result.stream().allMatch(n -> n.getChannel() == NotificationChannel.DASHBOARD));
    }

    @Test
    void shouldDispatchEmailAndDashboardForWarning() {
        var command = createCommand(AlertSeverity.WARNING);
        List<Notification> result = service.dispatchForAlert(command);

        Set<NotificationChannel> channels = new HashSet<>();
        result.forEach(n -> channels.add(n.getChannel()));
        assertTrue(channels.contains(NotificationChannel.EMAIL));
        assertTrue(channels.contains(NotificationChannel.DASHBOARD));
    }

    @Test
    void shouldDispatchAllChannelsForCritical() {
        var command = createCommand(AlertSeverity.CRITICAL);
        List<Notification> result = service.dispatchForAlert(command);

        Set<NotificationChannel> channels = new HashSet<>();
        result.forEach(n -> channels.add(n.getChannel()));
        assertTrue(channels.contains(NotificationChannel.SMS));
        assertTrue(channels.contains(NotificationChannel.PUSH));
        assertTrue(channels.contains(NotificationChannel.EMAIL));
        assertTrue(channels.contains(NotificationChannel.DASHBOARD));
    }

    @Test
    void shouldPersistNotifications() {
        var command = createCommand(AlertSeverity.WARNING);
        service.dispatchForAlert(command);

        assertFalse(repository.findAll().isEmpty());
    }

    @Test
    void shouldSkipDuplicates() {
        deduplication.alwaysDuplicate = true;
        var command = createCommand(AlertSeverity.WARNING);
        List<Notification> result = service.dispatchForAlert(command);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotSendToRecipientWithoutConsent() {
        recipientResolver.noConsent = true;
        var command = createCommand(AlertSeverity.CRITICAL);
        List<Notification> result = service.dispatchForAlert(command);

        assertTrue(result.stream().noneMatch(n -> n.getChannel() == NotificationChannel.SMS));
        assertTrue(result.stream().noneMatch(n -> n.getChannel() == NotificationChannel.PUSH));
        assertTrue(result.stream().noneMatch(n -> n.getChannel() == NotificationChannel.EMAIL));
    }

    @Test
    void shouldMarkNotificationAsSent() {
        var command = createCommand(AlertSeverity.WARNING);
        List<Notification> result = service.dispatchForAlert(command);

        assertTrue(result.stream().allMatch(n -> n.getStatus() == NotificationStatus.SENT));
    }

    private DispatchAlertNotificationCommand createCommand(AlertSeverity severity) {
        return new DispatchAlertNotificationCommand(
                new TenantId(UUID.randomUUID()),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                severity,
                "TEMPERATURE_RISE",
                "2025-01-15T10:30:00Z"
        );
    }

    // Test doubles
    static class InMemoryNotificationRepo implements NotificationRepositoryPort {
        private final List<Notification> store = new ArrayList<>();

        @Override public Notification save(Notification n) {
            store.removeIf(x -> x.getId().equals(n.getId()));
            store.add(n);
            return n;
        }
        @Override public Optional<Notification> findById(UUID id) {
            return store.stream().filter(n -> n.getId().equals(id)).findFirst();
        }
        @Override public List<Notification> findByRecipientId(UserId recipientId) {
            return store.stream().filter(n -> n.getRecipientId().equals(recipientId)).toList();
        }
        @Override public List<Notification> findByTenantId(TenantId tenantId) {
            return store.stream().filter(n -> n.getTenantId().equals(tenantId)).toList();
        }
        @Override public List<Notification> findByStatus(NotificationStatus status) {
            return store.stream().filter(n -> n.getStatus() == status).toList();
        }
        @Override public List<Notification> findPendingRetries() {
            return store.stream().filter(Notification::shouldRetryNow).toList();
        }
        @Override public long countByTenantIdAndStatus(TenantId tid, NotificationStatus status) {
            return store.stream().filter(n -> n.getTenantId().equals(tid) && n.getStatus() == status).count();
        }
        List<Notification> findAll() { return new ArrayList<>(store); }
    }

    static class FakeRecipientResolver implements RecipientResolverPort {
        boolean noConsent = false;
        @Override
        public List<Recipient> resolve(TenantId tenantId, List<RecipientType> types) {
            return types.stream().map(t -> new Recipient(
                    new UserId(UUID.randomUUID()), tenantId, t,
                    "test@test.com", "+33600000000", "push-token",
                    !noConsent, !noConsent, !noConsent
            )).toList();
        }
    }

    static class FakeDeduplication implements DeduplicationPort {
        boolean alwaysDuplicate = false;
        @Override public boolean isDuplicate(DeduplicationKey key) { return alwaysDuplicate; }
        @Override public void markSent(DeduplicationKey key) {}
    }

    static class FakeDispatcher extends NotificationDispatcher {
        FakeDispatcher() { super(new NoOpEmail(), new NoOpSms(), new NoOpPush(), new NoOpWebhook()); }
    }

    static class NoOpEmail implements EmailProviderPort {
        @Override public void send(String to, String subject, String body) {}
    }
    static class NoOpSms implements SmsProviderPort {
        @Override public void send(String phone, String msg) {}
    }
    static class NoOpPush implements PushProviderPort {
        @Override public void send(String token, String title, String body) {}
    }
    static class NoOpWebhook implements WebhookProviderPort {
        @Override public void send(String url, Map<String, Object> payload) {}
    }
}
