package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.ProcessNotificationEventUseCase;
import com.pyrosense.notification.application.port.in.SendNotificationUseCase;
import com.pyrosense.notification.application.port.in.SendNotificationUseCase.DispatchAlertNotificationCommand;
import com.pyrosense.notification.application.port.out.*;
import com.pyrosense.notification.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProcessNotificationEventService implements ProcessNotificationEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessNotificationEventService.class);

    private final SendNotificationUseCase sendUseCase;
    private final NotificationRepositoryPort repository;
    private final RecipientResolverPort recipientResolver;
    private final DeduplicationPort deduplication;
    private final NotificationDispatcher dispatcher;
    private final AuditLogPort auditLog;

    public ProcessNotificationEventService(SendNotificationUseCase sendUseCase,
                                            NotificationRepositoryPort repository,
                                            RecipientResolverPort recipientResolver,
                                            DeduplicationPort deduplication,
                                            NotificationDispatcher dispatcher,
                                            AuditLogPort auditLog) {
        this.sendUseCase = sendUseCase;
        this.repository = repository;
        this.recipientResolver = recipientResolver;
        this.deduplication = deduplication;
        this.dispatcher = dispatcher;
        this.auditLog = auditLog;
    }

    @Override
    public void processAlertCreated(AlertEventCommand command) {
        sendUseCase.dispatchForAlert(new DispatchAlertNotificationCommand(
                command.tenantId(), command.alertId(), command.deviceId(),
                command.severity(), command.alertType(), command.occurredAt()));
        auditLog.log("ALERT_NOTIFICATION_SENT", command.tenantId(),
                "alertId=%s severity=%s".formatted(command.alertId(), command.severity()));
    }

    @Override
    public void processAlertEscalated(AlertEventCommand command) {
        sendUseCase.dispatchForAlert(new DispatchAlertNotificationCommand(
                command.tenantId(), command.alertId(), command.deviceId(),
                command.severity(), command.alertType() + " [ESCALATION]", command.occurredAt()));
        auditLog.log("ALERT_ESCALATION_NOTIFICATION", command.tenantId(),
                "alertId=%s escalated".formatted(command.alertId()));
    }

    @Override
    public void processCriticalRiskDetected(CriticalRiskCommand command) {
        List<Recipient> recipients = recipientResolver.resolve(
                command.tenantId(), List.of(RecipientType.PROPERTY_MANAGER, RecipientType.TENANT_ADMIN));

        String subject = "[CRITIQUE] Score de risque critique détecté — %.0f/100".formatted(command.riskScore());
        String body = "Le dispositif %s du bâtiment %s a atteint un score de risque de %.0f/100. Une action immédiate est recommandée."
                .formatted(command.deviceId(), command.buildingId(), command.riskScore());

        dispatchToRecipients(command.tenantId(), recipients,
                Set.of(NotificationChannel.SMS, NotificationChannel.PUSH, NotificationChannel.EMAIL, NotificationChannel.DASHBOARD),
                AlertSeverity.CRITICAL, subject, body,
                "risk:%s:%s".formatted(command.deviceId(), command.occurredAt()));

        auditLog.log("CRITICAL_RISK_NOTIFICATION", command.tenantId(),
                "deviceId=%s score=%.0f".formatted(command.deviceId(), command.riskScore()));
    }

    @Override
    public void processInterventionCreated(InterventionEventCommand command) {
        List<Recipient> recipients = recipientResolver.resolve(
                command.tenantId(), List.of(RecipientType.PROPERTY_MANAGER, RecipientType.TENANT_ADMIN));

        String subject = "Nouvelle intervention créée — %s".formatted(command.interventionType());
        String body = "Une intervention de type %s a été créée pour le bâtiment %s."
                .formatted(command.interventionType(), command.buildingId());

        dispatchToRecipients(command.tenantId(), recipients,
                Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD),
                AlertSeverity.WARNING, subject, body,
                "intervention-created:%s".formatted(command.interventionId()));

        auditLog.log("INTERVENTION_CREATED_NOTIFICATION", command.tenantId(),
                "interventionId=%s".formatted(command.interventionId()));
    }

    @Override
    public void processInterventionAssigned(InterventionAssignedCommand command) {
        List<Recipient> recipients = recipientResolver.resolve(
                command.tenantId(), List.of(RecipientType.ELECTRICIAN));

        String subject = "Intervention assignée — %s".formatted(command.interventionType());
        String body = "Vous avez été assigné à une intervention de type %s sur le bâtiment %s."
                .formatted(command.interventionType(), command.buildingId());

        dispatchToRecipients(command.tenantId(), recipients,
                Set.of(NotificationChannel.PUSH, NotificationChannel.EMAIL, NotificationChannel.DASHBOARD),
                AlertSeverity.WARNING, subject, body,
                "intervention-assigned:%s:%s".formatted(command.interventionId(), command.assigneeId()));

        auditLog.log("INTERVENTION_ASSIGNED_NOTIFICATION", command.tenantId(),
                "interventionId=%s assignee=%s".formatted(command.interventionId(), command.assigneeId()));
    }

    @Override
    public void processInterventionCompleted(InterventionEventCommand command) {
        List<Recipient> recipients = recipientResolver.resolve(
                command.tenantId(), List.of(RecipientType.PROPERTY_MANAGER, RecipientType.TENANT_ADMIN));

        String subject = "Intervention terminée — %s".formatted(command.interventionType());
        String body = "L'intervention de type %s sur le bâtiment %s a été complétée."
                .formatted(command.interventionType(), command.buildingId());

        dispatchToRecipients(command.tenantId(), recipients,
                Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD),
                AlertSeverity.INFO, subject, body,
                "intervention-completed:%s".formatted(command.interventionId()));

        auditLog.log("INTERVENTION_COMPLETED_NOTIFICATION", command.tenantId(),
                "interventionId=%s".formatted(command.interventionId()));
    }

    @Override
    public void processReportGenerated(ReportGeneratedCommand command) {
        List<Recipient> recipients = recipientResolver.resolve(
                command.tenantId(), List.of(RecipientType.PROPERTY_MANAGER, RecipientType.TENANT_ADMIN));

        String subject = "Rapport disponible — %s (%s)".formatted(command.reportType(), command.reportNumber());
        String body = "Le rapport %s N° %s est prêt pour consultation."
                .formatted(command.reportType(), command.reportNumber());

        dispatchToRecipients(command.tenantId(), recipients,
                Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD),
                AlertSeverity.INFO, subject, body,
                "report:%s".formatted(command.reportId()));

        auditLog.log("REPORT_GENERATED_NOTIFICATION", command.tenantId(),
                "reportId=%s type=%s".formatted(command.reportId(), command.reportType()));
    }

    @Override
    public void processDeviceOffline(DeviceOfflineCommand command) {
        List<Recipient> recipients = recipientResolver.resolve(
                command.tenantId(), List.of(RecipientType.PROPERTY_MANAGER, RecipientType.TENANT_ADMIN));

        String subject = "[ALERTE] Capteur hors ligne — %s".formatted(command.deviceId());
        String body = "Le capteur %s du bâtiment %s est détecté hors ligne. Vérifiez la connectivité."
                .formatted(command.deviceId(), command.buildingId());

        dispatchToRecipients(command.tenantId(), recipients,
                Set.of(NotificationChannel.EMAIL, NotificationChannel.PUSH, NotificationChannel.DASHBOARD),
                AlertSeverity.WARNING, subject, body,
                "device-offline:%s:%s".formatted(command.deviceId(), command.occurredAt()));

        auditLog.log("DEVICE_OFFLINE_NOTIFICATION", command.tenantId(),
                "deviceId=%s building=%s".formatted(command.deviceId(), command.buildingId()));
    }

    @Override
    public void processDeviceBackOnline(DeviceBackOnlineCommand command) {
        List<Recipient> recipients = recipientResolver.resolve(
                command.tenantId(), List.of(RecipientType.PROPERTY_MANAGER, RecipientType.TENANT_ADMIN));

        String subject = "Capteur reconnecté — %s".formatted(command.deviceId());
        String body = "Le capteur %s du bâtiment %s est de nouveau opérationnel. Surveillance rétablie."
                .formatted(command.deviceId(), command.buildingId());

        dispatchToRecipients(command.tenantId(), recipients,
                Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD),
                AlertSeverity.INFO, subject, body,
                "device-online:%s:%s".formatted(command.deviceId(), command.occurredAt()));

        auditLog.log("DEVICE_BACK_ONLINE_NOTIFICATION", command.tenantId(),
                "deviceId=%s building=%s".formatted(command.deviceId(), command.buildingId()));
    }

    private void dispatchToRecipients(TenantId tenantId, List<Recipient> recipients,
                                       Set<NotificationChannel> channels, AlertSeverity severity,
                                       String subject, String body, String fingerprint) {
        for (Recipient recipient : recipients) {
            for (NotificationChannel channel : channels) {
                if (!recipient.canReceive(channel)) continue;

                Notification notification = new Notification(
                        UUID.randomUUID(), tenantId, recipient.userId(),
                        channel, severity, subject, body, fingerprint);

                DeduplicationKey key = notification.deduplicationKey();
                if (deduplication.isDuplicate(key)) {
                    log.debug("Skipping duplicate: recipient={} channel={} fingerprint={}",
                            recipient.userId().value(), channel, fingerprint);
                    continue;
                }

                repository.save(notification);
                dispatcher.dispatch(notification, recipient);
                repository.save(notification);
                deduplication.markSent(key);
            }
        }
    }
}
