package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.ProcessNotificationEventUseCase.*;
import com.pyrosense.notification.application.port.out.*;
import com.pyrosense.notification.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProcessNotificationEventServiceTest {

    private InMemoryNotificationRepo repository;
    private FakeRecipientResolver recipientResolver;
    private FakeDeduplication deduplication;
    private NotificationDispatcher dispatcher;
    private SendNotificationService sendService;
    private ProcessNotificationEventService service;
    private FakeAuditLog auditLog;

    private static final TenantId TENANT = new TenantId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepo();
        recipientResolver = new FakeRecipientResolver();
        deduplication = new FakeDeduplication();
        dispatcher = new NotificationDispatcher(
                new NoOpEmail(), new NoOpSms(), new NoOpPush(), new NoOpWebhook());
        sendService = new SendNotificationService(repository, recipientResolver, deduplication, dispatcher);
        auditLog = new FakeAuditLog();
        service = new ProcessNotificationEventService(
                sendService, repository, recipientResolver, deduplication, dispatcher, auditLog);
    }

    @Test
    void shouldProcessAlertCreated() {
        var cmd = new AlertEventCommand(TENANT, "alert-1", "device-1",
                AlertSeverity.WARNING, "TEMPERATURE_RISE", "2025-01-01T00:00:00Z");

        service.processAlertCreated(cmd);

        assertFalse(repository.findAll().isEmpty());
        assertTrue(auditLog.actions.contains("ALERT_NOTIFICATION_SENT"));
    }

    @Test
    void shouldProcessAlertEscalated() {
        var cmd = new AlertEventCommand(TENANT, "alert-1", "device-1",
                AlertSeverity.CRITICAL, "MICRO_ARC", "2025-01-01T00:00:00Z");

        service.processAlertEscalated(cmd);

        assertFalse(repository.findAll().isEmpty());
        assertTrue(auditLog.actions.contains("ALERT_ESCALATION_NOTIFICATION"));
    }

    @Test
    void shouldProcessCriticalRiskDetected() {
        var cmd = new CriticalRiskCommand(TENANT, "device-1", "building-1", 85.0, "2025-01-01T00:00:00Z");

        service.processCriticalRiskDetected(cmd);

        assertFalse(repository.findAll().isEmpty());
        assertTrue(auditLog.actions.contains("CRITICAL_RISK_NOTIFICATION"));
    }

    @Test
    void shouldProcessInterventionCreated() {
        var cmd = new InterventionEventCommand(TENANT, "int-1", "building-1", "PREVENTIVE", "2025-01-01T00:00:00Z");

        service.processInterventionCreated(cmd);

        assertFalse(repository.findAll().isEmpty());
        assertTrue(auditLog.actions.contains("INTERVENTION_CREATED_NOTIFICATION"));
    }

    @Test
    void shouldProcessInterventionAssigned() {
        var cmd = new InterventionAssignedCommand(TENANT, "int-1", "building-1",
                "elec-1", "CORRECTIVE", "2025-01-01T00:00:00Z");

        service.processInterventionAssigned(cmd);

        assertFalse(repository.findAll().isEmpty());
        assertTrue(auditLog.actions.contains("INTERVENTION_ASSIGNED_NOTIFICATION"));
    }

    @Test
    void shouldProcessInterventionCompleted() {
        var cmd = new InterventionEventCommand(TENANT, "int-1", "building-1", "PREVENTIVE", "2025-01-01T00:00:00Z");

        service.processInterventionCompleted(cmd);

        assertFalse(repository.findAll().isEmpty());
        assertTrue(auditLog.actions.contains("INTERVENTION_COMPLETED_NOTIFICATION"));
    }

    @Test
    void shouldProcessReportGenerated() {
        var cmd = new ReportGeneratedCommand(TENANT, "report-1", "MH-202501-00001",
                "MONTHLY_HEALTH", "building-1", "2025-01-01T00:00:00Z");

        service.processReportGenerated(cmd);

        assertFalse(repository.findAll().isEmpty());
        assertTrue(auditLog.actions.contains("REPORT_GENERATED_NOTIFICATION"));
    }

    @Test
    void shouldProcessDeviceOffline() {
        var cmd = new DeviceOfflineCommand(TENANT, "device-1", "building-1", "2025-01-01T00:00:00Z");

        service.processDeviceOffline(cmd);

        assertFalse(repository.findAll().isEmpty());
        assertTrue(auditLog.actions.contains("DEVICE_OFFLINE_NOTIFICATION"));
    }

    @Test
    void shouldNotSendDuplicateForSameEvent() {
        var cmd = new AlertEventCommand(TENANT, "alert-1", "device-1",
                AlertSeverity.WARNING, "TEMPERATURE_RISE", "2025-01-01T00:00:00Z");

        service.processAlertCreated(cmd);
        int firstCount = repository.findAll().size();

        service.processAlertCreated(cmd);
        int secondCount = repository.findAll().size();

        assertEquals(firstCount, secondCount);
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
        private final Map<RecipientType, UserId> stableIds = new HashMap<>();
        @Override
        public List<Recipient> resolve(TenantId tenantId, List<RecipientType> types) {
            return types.stream().map(t -> {
                UserId uid = stableIds.computeIfAbsent(t, k -> new UserId(UUID.randomUUID()));
                return new Recipient(uid, tenantId, t, "test@test.com", "+33600000000", "push-token", true, true, true);
            }).toList();
        }
    }

    static class FakeDeduplication implements DeduplicationPort {
        private final Set<String> seen = new HashSet<>();
        @Override public boolean isDuplicate(DeduplicationKey key) { return seen.contains(key.toKeyString()); }
        @Override public void markSent(DeduplicationKey key) { seen.add(key.toKeyString()); }
    }

    static class FakeAuditLog implements AuditLogPort {
        final List<String> actions = new ArrayList<>();
        @Override public void log(String action, TenantId tenantId, String details) { actions.add(action); }
    }

    static class NoOpEmail implements EmailProviderPort { @Override public void send(String to, String s, String b) {} }
    static class NoOpSms implements SmsProviderPort { @Override public void send(String p, String m) {} }
    static class NoOpPush implements PushProviderPort { @Override public void send(String t, String ti, String b) {} }
    static class NoOpWebhook implements WebhookProviderPort { @Override public void send(String u, Map<String, Object> p) {} }
}
