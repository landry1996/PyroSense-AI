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

import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PreferenceBypassTest {

    private InMemoryRepo repository;
    private FixedRecipientResolver recipientResolver;
    private SendNotificationService service;
    private InMemoryPreferencesRepo preferencesRepo;
    private UserId recipientUserId;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepo();
        recipientUserId = new UserId(UUID.randomUUID());
        tenantId = new TenantId(UUID.randomUUID());
        recipientResolver = new FixedRecipientResolver(recipientUserId, tenantId);
        preferencesRepo = new InMemoryPreferencesRepo();
        var dispatcher = new NotificationDispatcher(
                (to, s, b) -> {}, (p, m) -> {}, (t, ti, b) -> {}, (u, payload) -> {});
        service = new SendNotificationService(repository, recipientResolver,
                new NoDedup(), dispatcher, preferencesRepo);
    }

    @Test
    void shouldRespectDisabledEmailPreference() {
        NotificationPreferences prefs = new NotificationPreferences(recipientUserId, tenantId);
        prefs.update(false, true, true, true, null, null);
        preferencesRepo.save(prefs);

        var command = createCommand(AlertSeverity.WARNING);
        List<Notification> result = service.dispatchForAlert(command);

        assertTrue(result.stream().noneMatch(n -> n.getChannel() == NotificationChannel.EMAIL));
        assertTrue(result.stream().anyMatch(n -> n.getChannel() == NotificationChannel.DASHBOARD));
    }

    @Test
    void shouldBypassPreferencesForCritical() {
        NotificationPreferences prefs = new NotificationPreferences(recipientUserId, tenantId);
        prefs.updateFull(false, false, false, false, null, null, "fr", true, true, true, true);
        preferencesRepo.save(prefs);

        var command = createCommand(AlertSeverity.CRITICAL);
        List<Notification> result = service.dispatchForAlert(command);

        assertTrue(result.stream().anyMatch(n -> n.getChannel() == NotificationChannel.EMAIL));
        assertTrue(result.stream().anyMatch(n -> n.getChannel() == NotificationChannel.SMS));
        assertTrue(result.stream().anyMatch(n -> n.getChannel() == NotificationChannel.PUSH));
    }

    @Test
    void shouldAlwaysAllowDashboard() {
        NotificationPreferences prefs = new NotificationPreferences(recipientUserId, tenantId);
        prefs.update(false, false, false, false, null, null);
        preferencesRepo.save(prefs);

        var command = createCommand(AlertSeverity.WARNING);
        List<Notification> result = service.dispatchForAlert(command);

        assertTrue(result.stream().anyMatch(n -> n.getChannel() == NotificationChannel.DASHBOARD));
    }

    @Test
    void shouldSendAllChannelsWhenNoPreferencesExist() {
        var command = createCommand(AlertSeverity.CRITICAL);
        List<Notification> result = service.dispatchForAlert(command);

        Set<NotificationChannel> channels = new HashSet<>();
        result.forEach(n -> channels.add(n.getChannel()));
        assertTrue(channels.contains(NotificationChannel.SMS));
        assertTrue(channels.contains(NotificationChannel.PUSH));
        assertTrue(channels.contains(NotificationChannel.EMAIL));
        assertTrue(channels.contains(NotificationChannel.DASHBOARD));
    }

    private DispatchAlertNotificationCommand createCommand(AlertSeverity severity) {
        return new DispatchAlertNotificationCommand(tenantId, "alert-1", "device-1",
                severity, "TEMPERATURE_RISE", "2025-01-01T00:00:00Z");
    }

    static class InMemoryRepo implements NotificationRepositoryPort {
        private final List<Notification> store = new ArrayList<>();
        @Override public Notification save(Notification n) { store.removeIf(x -> x.getId().equals(n.getId())); store.add(n); return n; }
        @Override public Optional<Notification> findById(UUID id) { return store.stream().filter(n -> n.getId().equals(id)).findFirst(); }
        @Override public List<Notification> findByRecipientId(UserId r) { return List.of(); }
        @Override public List<Notification> findByTenantId(TenantId t) { return List.of(); }
        @Override public List<Notification> findByStatus(NotificationStatus s) { return List.of(); }
        @Override public List<Notification> findPendingRetries() { return List.of(); }
        @Override public long countByTenantIdAndStatus(TenantId t, NotificationStatus s) { return 0; }
    }

    static class FixedRecipientResolver implements RecipientResolverPort {
        private final UserId userId;
        private final TenantId tenantId;
        FixedRecipientResolver(UserId userId, TenantId tenantId) { this.userId = userId; this.tenantId = tenantId; }
        @Override
        public List<Recipient> resolve(TenantId tid, List<RecipientType> types) {
            return List.of(new Recipient(userId, tenantId, RecipientType.PROPERTY_MANAGER,
                    "pm@test.com", "+33600000000", "push-token-pm", true, true, true));
        }
    }

    static class InMemoryPreferencesRepo implements NotificationPreferencesRepository {
        private final Map<UUID, NotificationPreferences> store = new HashMap<>();
        @Override public Optional<NotificationPreferences> findByUserId(UserId userId) {
            return Optional.ofNullable(store.get(userId.value()));
        }
        @Override public NotificationPreferences save(NotificationPreferences prefs) {
            store.put(prefs.getUserId().value(), prefs);
            return prefs;
        }
    }

    static class NoDedup implements DeduplicationPort {
        @Override public boolean isDuplicate(DeduplicationKey key) { return false; }
        @Override public void markSent(DeduplicationKey key) {}
    }
}
