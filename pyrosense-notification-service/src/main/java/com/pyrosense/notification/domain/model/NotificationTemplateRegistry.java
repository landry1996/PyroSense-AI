package com.pyrosense.notification.domain.model;

import com.pyrosense.notification.domain.model.NotificationTemplateDefinition.ChannelContent;
import com.pyrosense.notification.domain.model.NotificationTemplateDefinition.PrivacyRule;

import java.util.*;

import static com.pyrosense.notification.domain.model.NotificationTemplateCode.*;

public final class NotificationTemplateRegistry {

    private NotificationTemplateRegistry() {}

    private static final Map<NotificationTemplateCode, NotificationTemplateDefinition> TEMPLATES;

    static {
        Map<NotificationTemplateCode, NotificationTemplateDefinition> map = new EnumMap<>(NotificationTemplateCode.class);

        map.put(ALERT_WARNING, new NotificationTemplateDefinition(
                ALERT_WARNING,
                "Alerte de surveillance",
                new ChannelContent(
                        "Alerte {alertType}",
                        "Une anomalie de type {alertType} a été détectée sur le capteur {deviceId}. Surveillance renforcée recommandée."
                ),
                new ChannelContent(
                        "[PyroSense] Attention requise — {alertType}",
                        """
                        Bonjour,

                        Notre système de surveillance a détecté une anomalie nécessitant votre attention.

                        Type d'anomalie : {alertType}
                        Bâtiment concerné : {buildingId}
                        Capteur : {deviceId}
                        Date de détection : {occurredAt}

                        Action recommandée : vérifiez l'état de l'installation dans les 48 heures.

                        Vous pouvez consulter les détails sur votre tableau de bord PyroSense.

                        Cordialement,
                        L'équipe PyroSense AI Platform

                        ---
                        Ce message est généré automatiquement par le système de monitoring prédictif PyroSense.
                        Il constitue une aide à la décision et ne se substitue pas à un diagnostic professionnel."""
                ),
                new ChannelContent(
                        "PyroSense - Alerte",
                        "PyroSense: Anomalie {alertType} détectée. Vérification recommandée sous 48h. Consultez votre tableau de bord."
                ),
                new ChannelContent(
                        "Attention requise",
                        "Anomalie {alertType} détectée sur votre installation. Action recommandée sous 48h."
                ),
                NotificationPriority.MEDIUM,
                "Consulter le tableau de bord",
                Set.of(PrivacyRule.NO_RAW_ELECTRICAL_DATA, PrivacyRule.NO_GUARANTEE_FIRE_PREDICTION),
                List.of("alertType", "deviceId", "buildingId", "occurredAt")
        ));

        map.put(ALERT_CRITICAL, new NotificationTemplateDefinition(
                ALERT_CRITICAL,
                "Alerte critique — Intervention recommandée",
                new ChannelContent(
                        "CRITIQUE — {alertType}",
                        "Situation critique détectée sur {buildingId}. Une intervention professionnelle est recommandée. Consultez le tableau de bord pour les détails."
                ),
                new ChannelContent(
                        "[PyroSense] Situation critique — Intervention professionnelle recommandée",
                        """
                        Bonjour,

                        Notre système de surveillance a identifié une situation nécessitant une attention prioritaire.

                        Bâtiment concerné : {buildingId}
                        Type de situation : {alertType}
                        Date de détection : {occurredAt}

                        RECOMMANDATIONS :
                        1. Consultez votre tableau de bord PyroSense pour les détails de la situation.
                        2. Contactez un professionnel qualifié pour une inspection sur site.
                        3. En cas de doute sur la sécurité immédiate, appelez les services d'urgence.

                        IMPORTANT :
                        - Cette notification constitue une aide à la décision basée sur l'analyse prédictive.
                        - Elle ne garantit pas la survenue d'un incident.
                        - Seul un professionnel qualifié peut établir un diagnostic définitif.

                        Ne prenez aucune décision de sécurité basée uniquement sur cette notification.

                        Cordialement,
                        L'équipe PyroSense AI Platform

                        ---
                        PyroSense AI Platform — Monitoring prédictif des risques électriques.
                        Aide à la décision. Ne se substitue pas à un diagnostic professionnel."""
                ),
                new ChannelContent(
                        "PyroSense CRITIQUE",
                        "PyroSense: Situation critique sur {buildingId}. Contactez un professionnel qualifié. Détails sur votre tableau de bord."
                ),
                new ChannelContent(
                        "Situation critique détectée",
                        "Situation critique sur {buildingId}. Intervention professionnelle recommandée. Consultez votre tableau de bord."
                ),
                NotificationPriority.URGENT,
                "Contacter un professionnel qualifié",
                Set.of(PrivacyRule.NO_RAW_ELECTRICAL_DATA, PrivacyRule.NO_GUARANTEE_FIRE_PREDICTION, PrivacyRule.NO_EXACT_LOCATION),
                List.of("alertType", "buildingId", "occurredAt")
        ));

        map.put(CRITICAL_RISK_DETECTED, new NotificationTemplateDefinition(
                CRITICAL_RISK_DETECTED,
                "Score de risque critique",
                new ChannelContent(
                        "Risque critique — {buildingId}",
                        "Score de risque élevé détecté sur {buildingId}. Intervention professionnelle recommandée."
                ),
                new ChannelContent(
                        "[PyroSense] Score de risque critique — Intervention recommandée",
                        """
                        Bonjour,

                        L'analyse prédictive de votre installation a identifié un niveau de risque nécessitant une attention prioritaire.

                        Bâtiment concerné : {buildingId}
                        Niveau de risque : {riskScore}/100
                        Date d'évaluation : {occurredAt}

                        RECOMMANDATIONS :
                        1. Planifiez une inspection par un professionnel qualifié dans les meilleurs délais.
                        2. Consultez votre tableau de bord pour le détail de l'évaluation.
                        3. En cas de signes visibles (odeur, chaleur, bruit anormal), contactez immédiatement les services d'urgence.

                        IMPORTANT :
                        - Ce score est une estimation probabiliste basée sur les capteurs de monitoring.
                        - Il ne constitue pas un diagnostic et ne garantit pas la survenue d'un incident.
                        - Seul un professionnel qualifié peut établir un diagnostic définitif.

                        Cordialement,
                        L'équipe PyroSense AI Platform"""
                ),
                new ChannelContent(
                        "PyroSense CRITIQUE",
                        "PyroSense: Risque critique sur {buildingId}. Contactez un professionnel. Détails: tableau de bord."
                ),
                new ChannelContent(
                        "Risque critique détecté",
                        "Risque critique sur {buildingId}. Intervention professionnelle recommandée."
                ),
                NotificationPriority.URGENT,
                "Planifier une inspection professionnelle",
                Set.of(PrivacyRule.NO_RAW_ELECTRICAL_DATA, PrivacyRule.NO_GUARANTEE_FIRE_PREDICTION, PrivacyRule.NO_EXACT_LOCATION),
                List.of("buildingId", "riskScore", "occurredAt")
        ));

        map.put(INTERVENTION_CREATED, new NotificationTemplateDefinition(
                INTERVENTION_CREATED,
                "Intervention planifiée",
                new ChannelContent(
                        "Intervention créée — {interventionType}",
                        "Intervention {interventionType} créée pour {buildingId}."
                ),
                new ChannelContent(
                        "[PyroSense] Intervention planifiée — {interventionType}",
                        """
                        Bonjour,

                        Une intervention a été programmée suite à l'analyse de votre installation.

                        Type d'intervention : {interventionType}
                        Bâtiment concerné : {buildingId}
                        Date de création : {occurredAt}

                        Un technicien qualifié sera assigné prochainement.
                        Vous recevrez une notification lorsque l'intervention sera planifiée.

                        Consultez votre tableau de bord pour suivre l'avancement.

                        Cordialement,
                        L'équipe PyroSense AI Platform"""
                ),
                new ChannelContent(
                        "PyroSense",
                        "PyroSense: Intervention {interventionType} créée pour {buildingId}. Suivi sur votre tableau de bord."
                ),
                new ChannelContent(
                        "Intervention planifiée",
                        "Intervention {interventionType} planifiée pour {buildingId}."
                ),
                NotificationPriority.MEDIUM,
                "Suivre l'intervention",
                Set.of(PrivacyRule.NO_RAW_ELECTRICAL_DATA),
                List.of("interventionType", "buildingId", "occurredAt")
        ));

        map.put(INTERVENTION_ASSIGNED, new NotificationTemplateDefinition(
                INTERVENTION_ASSIGNED,
                "Intervention assignée",
                new ChannelContent(
                        "Intervention assignée — {interventionType}",
                        "Vous êtes assigné à l'intervention {interventionType} sur {buildingId}."
                ),
                new ChannelContent(
                        "[PyroSense] Intervention assignée — {interventionType}",
                        """
                        Bonjour,

                        Vous avez été assigné à une intervention sur une installation surveillée.

                        Type d'intervention : {interventionType}
                        Bâtiment concerné : {buildingId}
                        Date d'assignation : {occurredAt}

                        Veuillez consulter votre tableau de bord PyroSense pour les détails techniques
                        et l'historique des capteurs concernés.

                        Cordialement,
                        L'équipe PyroSense AI Platform"""
                ),
                new ChannelContent(
                        "PyroSense",
                        "PyroSense: Vous êtes assigné — intervention {interventionType} sur {buildingId}."
                ),
                new ChannelContent(
                        "Intervention assignée",
                        "Nouvelle intervention {interventionType} sur {buildingId} vous est assignée."
                ),
                NotificationPriority.HIGH,
                "Consulter les détails de l'intervention",
                Set.of(PrivacyRule.NO_RAW_ELECTRICAL_DATA, PrivacyRule.NO_PERSONAL_INFO_IN_PUSH),
                List.of("interventionType", "buildingId", "occurredAt")
        ));

        map.put(INTERVENTION_COMPLETED, new NotificationTemplateDefinition(
                INTERVENTION_COMPLETED,
                "Intervention terminée",
                new ChannelContent(
                        "Intervention terminée — {interventionType}",
                        "L'intervention {interventionType} sur {buildingId} est terminée."
                ),
                new ChannelContent(
                        "[PyroSense] Intervention terminée — {interventionType}",
                        """
                        Bonjour,

                        L'intervention sur votre installation a été complétée.

                        Type d'intervention : {interventionType}
                        Bâtiment : {buildingId}
                        Date de complétion : {occurredAt}

                        Vous pouvez consulter le rapport de l'intervention sur votre tableau de bord.
                        Le système de monitoring continuera la surveillance pour vérifier l'efficacité de l'intervention.

                        Cordialement,
                        L'équipe PyroSense AI Platform"""
                ),
                new ChannelContent(
                        "PyroSense",
                        "PyroSense: Intervention {interventionType} terminée sur {buildingId}. Consultez le rapport."
                ),
                new ChannelContent(
                        "Intervention complétée",
                        "Intervention {interventionType} terminée sur {buildingId}."
                ),
                NotificationPriority.LOW,
                "Consulter le rapport d'intervention",
                Set.of(PrivacyRule.NO_RAW_ELECTRICAL_DATA),
                List.of("interventionType", "buildingId", "occurredAt")
        ));

        map.put(REPORT_GENERATED, new NotificationTemplateDefinition(
                REPORT_GENERATED,
                "Rapport disponible",
                new ChannelContent(
                        "Rapport {reportType} disponible",
                        "Le rapport {reportType} N° {reportNumber} est prêt."
                ),
                new ChannelContent(
                        "[PyroSense] Rapport disponible — {reportType} N° {reportNumber}",
                        """
                        Bonjour,

                        Un nouveau rapport est disponible pour consultation.

                        Type de rapport : {reportType}
                        Numéro : {reportNumber}
                        Bâtiment : {buildingId}
                        Date de génération : {occurredAt}

                        Vous pouvez télécharger ce rapport depuis votre tableau de bord PyroSense,
                        section « Rapports ».

                        Cordialement,
                        L'équipe PyroSense AI Platform"""
                ),
                new ChannelContent(
                        "PyroSense",
                        "PyroSense: Rapport {reportType} N°{reportNumber} disponible. Téléchargez-le depuis votre tableau de bord."
                ),
                new ChannelContent(
                        "Rapport disponible",
                        "Rapport {reportType} N° {reportNumber} disponible."
                ),
                NotificationPriority.LOW,
                "Télécharger le rapport",
                Set.of(PrivacyRule.NO_RAW_ELECTRICAL_DATA),
                List.of("reportType", "reportNumber", "occurredAt")
        ));

        map.put(DEVICE_OFFLINE, new NotificationTemplateDefinition(
                DEVICE_OFFLINE,
                "Capteur hors ligne",
                new ChannelContent(
                        "Capteur hors ligne — {deviceId}",
                        "Le capteur {deviceId} de {buildingId} ne répond plus. Vérifiez la connectivité."
                ),
                new ChannelContent(
                        "[PyroSense] Capteur hors ligne — Vérification requise",
                        """
                        Bonjour,

                        Un capteur de votre installation ne répond plus au système de monitoring.

                        Capteur : {deviceId}
                        Bâtiment : {buildingId}
                        Dernière communication : {occurredAt}

                        RECOMMANDATIONS :
                        1. Vérifiez l'alimentation électrique du capteur.
                        2. Vérifiez la connexion réseau (WiFi/Ethernet).
                        3. Si le problème persiste, contactez le support technique.

                        IMPORTANT : pendant la période d'indisponibilité, la zone concernée
                        n'est plus surveillée par le système de monitoring prédictif.

                        Cordialement,
                        L'équipe PyroSense AI Platform"""
                ),
                new ChannelContent(
                        "PyroSense",
                        "PyroSense: Capteur {deviceId} hors ligne sur {buildingId}. Zone non surveillée. Vérifiez la connectivité."
                ),
                new ChannelContent(
                        "Capteur hors ligne",
                        "Capteur {deviceId} hors ligne. Zone non surveillée. Vérifiez la connectivité."
                ),
                NotificationPriority.HIGH,
                "Vérifier l'état du capteur",
                Set.of(PrivacyRule.MASK_DEVICE_ID_IN_SMS, PrivacyRule.NO_EXACT_LOCATION),
                List.of("deviceId", "buildingId", "occurredAt")
        ));

        map.put(DEVICE_BACK_ONLINE, new NotificationTemplateDefinition(
                DEVICE_BACK_ONLINE,
                "Capteur de nouveau en ligne",
                new ChannelContent(
                        "Capteur reconnecté — {deviceId}",
                        "Le capteur {deviceId} de {buildingId} est de nouveau opérationnel."
                ),
                new ChannelContent(
                        "[PyroSense] Capteur reconnecté — Surveillance rétablie",
                        """
                        Bonjour,

                        Un capteur précédemment hors ligne est de nouveau opérationnel.

                        Capteur : {deviceId}
                        Bâtiment : {buildingId}
                        Reconnexion : {occurredAt}

                        La surveillance de la zone concernée est rétablie.
                        Aucune action de votre part n'est nécessaire.

                        Cordialement,
                        L'équipe PyroSense AI Platform"""
                ),
                new ChannelContent(
                        "PyroSense",
                        "PyroSense: Capteur {deviceId} reconnecté sur {buildingId}. Surveillance rétablie."
                ),
                new ChannelContent(
                        "Capteur reconnecté",
                        "Capteur {deviceId} de nouveau en ligne. Surveillance rétablie."
                ),
                NotificationPriority.LOW,
                "Aucune action requise",
                Set.of(PrivacyRule.MASK_DEVICE_ID_IN_SMS),
                List.of("deviceId", "buildingId", "occurredAt")
        ));

        TEMPLATES = Collections.unmodifiableMap(map);
    }

    public static NotificationTemplateDefinition get(NotificationTemplateCode code) {
        NotificationTemplateDefinition def = TEMPLATES.get(code);
        if (def == null) {
            throw new IllegalArgumentException("No template defined for code: " + code);
        }
        return def;
    }

    public static Map<NotificationTemplateCode, NotificationTemplateDefinition> all() {
        return TEMPLATES;
    }
}
