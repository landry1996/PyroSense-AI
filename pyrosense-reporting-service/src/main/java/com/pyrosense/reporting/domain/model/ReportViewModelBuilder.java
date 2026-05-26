package com.pyrosense.reporting.domain.model;

import com.pyrosense.reporting.domain.model.ReportViewModel.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ReportViewModelBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneOffset.UTC);
    private static final String PLATFORM_VERSION = "1.0.0";
    private static final String GENERATED_BY = "PyroSense AI Platform";

    private static final LegalDisclaimer STANDARD_DISCLAIMER = new LegalDisclaimer(
            "Ce rapport est un outil d'aide à la décision basé sur le monitoring prédictif " +
                    "des installations électriques. Il ne constitue en aucun cas un diagnostic " +
                    "réglementaire au sens des normes NF C 15-100 ou NF C 18-510, ni une garantie " +
                    "absolue d'absence de risque d'incendie d'origine électrique.",
            List.of(
                    "Les données présentées sont issues d'une analyse algorithmique et statistique des mesures capteurs.",
                    "Une recommandation d'inspection ne se substitue pas à un contrôle par un organisme agréé.",
                    "Les scores de risque sont des indicateurs probabilistes, non des certitudes.",
                    "PyroSense AI ne peut être tenu responsable des décisions prises sur la seule base de ce rapport."
            )
    );

    private ReportViewModelBuilder() {}

    public static ReportViewModel buildMonthlyHealth(String reportNumber, ReportMetadata metadata,
                                                      Instant periodStart, Instant periodEnd, Instant generatedAt) {
        String periodLabel = "%s au %s".formatted(DATE_FMT.format(periodStart), DATE_FMT.format(periodEnd));
        RiskLevel risk = classifyRisk(metadata.averageRiskScore());

        List<Section> sections = new ArrayList<>();

        sections.add(buildExecutiveSummary(metadata, periodLabel));
        sections.add(buildKpiSection(metadata));
        sections.add(buildRiskAnalysisSection(metadata));
        sections.add(buildAlertSection(metadata));
        sections.add(buildInterventionSection(metadata));
        sections.add(buildRecommendationsSection(metadata));

        return new ReportViewModel(
                new Header("Bilan Mensuel de Santé Électrique", reportNumber,
                        metadata.buildingName(), metadata.buildingName(), metadata.buildingAddress(),
                        periodLabel, generatedAt, risk),
                ReportType.MONTHLY_HEALTH, sections, STANDARD_DISCLAIMER,
                buildSignature(reportNumber, generatedAt),
                new Footer(GENERATED_BY, PLATFORM_VERSION, generatedAt, 0)
        );
    }

    public static ReportViewModel buildMonitoringCertificate(String reportNumber, ReportMetadata metadata,
                                                              Instant periodStart, Instant periodEnd, Instant generatedAt) {
        String periodLabel = "%s au %s".formatted(DATE_FMT.format(periodStart), DATE_FMT.format(periodEnd));
        int totalSensors = metadata.sensorCount() + metadata.offlineSensorCount();
        double availability = totalSensors > 0 ? (metadata.sensorCount() * 100.0 / totalSensors) : 100.0;
        String monitoringStatus = metadata.offlineSensorCount() == 0 ? "ACTIF" : "PARTIELLEMENT DÉGRADÉ";

        List<Section> sections = new ArrayList<>();

        sections.add(new Section("Objet de l'attestation", SectionType.CERTIFICATION,
                List.of(), List.of(), List.of(),
                "La présente attestation certifie que le bâtiment « %s » situé au %s ".formatted(
                        metadata.buildingName(), metadata.buildingAddress()) +
                        "a fait l'objet d'une surveillance électrique continue par le système PyroSense AI " +
                        "pendant la période du %s.".formatted(periodLabel)));

        sections.add(new Section("Paramètres de surveillance", SectionType.KPI_INDICATORS,
                List.of(
                        new KeyValue("Capteurs déployés", String.valueOf(totalSensors), Severity.INFO),
                        new KeyValue("Capteurs actifs", String.valueOf(metadata.sensorCount()),
                                metadata.offlineSensorCount() > 0 ? Severity.WARNING : Severity.NORMAL),
                        new KeyValue("Capteurs hors ligne", String.valueOf(metadata.offlineSensorCount()),
                                metadata.offlineSensorCount() > 0 ? Severity.WARNING : Severity.NORMAL),
                        new KeyValue("Taux de disponibilité", "%.1f%%".formatted(availability),
                                availability < 95 ? Severity.WARNING : Severity.NORMAL),
                        new KeyValue("Statut monitoring", monitoringStatus,
                                metadata.offlineSensorCount() == 0 ? Severity.NORMAL : Severity.WARNING)
                ), List.of(), List.of(), null));

        sections.add(new Section("Synthèse des détections", SectionType.ALERT_SUMMARY,
                List.of(
                        new KeyValue("Alertes détectées", String.valueOf(metadata.alertCount()), Severity.INFO),
                        new KeyValue("Alertes critiques", String.valueOf(metadata.criticalAlertCount()),
                                metadata.criticalAlertCount() > 0 ? Severity.CRITICAL : Severity.NORMAL),
                        new KeyValue("Interventions déclenchées", String.valueOf(metadata.interventionCount()), Severity.INFO),
                        new KeyValue("Défauts confirmés", String.valueOf(metadata.confirmedDefects()),
                                metadata.confirmedDefects() > 0 ? Severity.WARNING : Severity.NORMAL)
                ), List.of(), List.of(), null));

        sections.add(new Section("Conclusion", SectionType.CERTIFICATION,
                List.of(), List.of(), List.of(),
                "Le système de monitoring a fonctionné de manière %s pendant la période couverte. ".formatted(
                        availability >= 99 ? "nominale" : availability >= 95 ? "satisfaisante" : "dégradée") +
                        "Ce certificat atteste de la continuité de la surveillance, non de l'absence de risque."));

        LegalDisclaimer certDisclaimer = new LegalDisclaimer(
                "Cette attestation est un outil d'aide à la décision délivré dans le cadre du monitoring prédictif. " +
                        "Elle ne constitue en aucun cas un diagnostic réglementaire et ne se substitue pas aux " +
                        "vérifications périodiques obligatoires (décret n°2010-1016) ni aux diagnostics réalisés " +
                        "par un organisme de contrôle agréé.",
                List.of(
                        "Le taux de disponibilité reflète le fonctionnement des capteurs, non la couverture exhaustive de l'installation.",
                        "Une attestation de monitoring continu est un complément, non un remplacement, de la conformité réglementaire."
                )
        );

        return new ReportViewModel(
                new Header("Attestation de Surveillance Électrique Continue", reportNumber,
                        metadata.buildingName(), metadata.buildingName(), metadata.buildingAddress(),
                        periodLabel, generatedAt, classifyRisk(metadata.averageRiskScore())),
                ReportType.CONTINUOUS_MONITORING_CERTIFICATE, sections, certDisclaimer,
                buildSignature(reportNumber, generatedAt),
                new Footer(GENERATED_BY, PLATFORM_VERSION, generatedAt, 0)
        );
    }

    public static ReportViewModel buildCriticalAlert(String reportNumber, ReportMetadata metadata,
                                                      Instant periodStart, Instant periodEnd, Instant generatedAt) {
        String periodLabel = "%s au %s".formatted(DATE_FMT.format(periodStart), DATE_FMT.format(periodEnd));

        List<Section> sections = new ArrayList<>();

        sections.add(new Section("Nature de l'alerte", SectionType.DIAGNOSTIC,
                List.of(
                        new KeyValue("Sévérité", "CRITIQUE", Severity.CRITICAL),
                        new KeyValue("Score de risque", "%.0f / 100".formatted(metadata.averageRiskScore()),
                                classifySeverity(metadata.averageRiskScore())),
                        new KeyValue("Bâtiment concerné", metadata.buildingName(), Severity.INFO),
                        new KeyValue("Adresse", metadata.buildingAddress(), Severity.INFO)
                ), List.of(), List.of(), null));

        sections.add(new Section("Contexte de détection", SectionType.TIMELINE,
                List.of(
                        new KeyValue("Période d'analyse", periodLabel, Severity.INFO),
                        new KeyValue("Alertes sur la période", String.valueOf(metadata.alertCount()), Severity.INFO),
                        new KeyValue("Alertes critiques", String.valueOf(metadata.criticalAlertCount()), Severity.CRITICAL)
                ), List.of(), List.of(),
                "Cette alerte a été identifiée par le système de monitoring prédictif PyroSense AI " +
                        "sur la base d'une analyse statistique des mesures capteurs en continu."));

        sections.add(new Section("Évaluation du risque", SectionType.RISK_ANALYSIS,
                List.of(
                        new KeyValue("Niveau de risque global", classifyRisk(metadata.averageRiskScore()).name(),
                                classifySeverity(metadata.averageRiskScore())),
                        new KeyValue("Évolution du risque", "%.1f%%".formatted(metadata.riskEvolutionPercent()),
                                metadata.riskEvolutionPercent() > 0 ? Severity.WARNING : Severity.NORMAL),
                        new KeyValue("Défauts confirmés antérieurs", String.valueOf(metadata.confirmedDefects()), Severity.INFO)
                ), List.of(), List.of(), null));

        sections.add(new Section("Actions recommandées", SectionType.RECOMMENDATIONS,
                List.of(), List.of(),
                List.of(
                        "Inspection visuelle immédiate de l'installation concernée par un électricien qualifié.",
                        "Vérification des connexions et serrages au tableau électrique.",
                        "Contrôle thermographique des points chauds identifiés.",
                        "Planification d'une intervention corrective si le défaut est confirmé."
                ),
                "Ces recommandations sont des préconisations d'inspection basées sur l'analyse algorithmique. " +
                        "Seul un professionnel qualifié peut confirmer ou infirmer la présence d'un défaut."));

        return new ReportViewModel(
                new Header("Rapport d'Alerte Critique", reportNumber,
                        metadata.buildingName(), metadata.buildingName(), metadata.buildingAddress(),
                        periodLabel, generatedAt, RiskLevel.CRITICAL),
                ReportType.CRITICAL_ALERT_REPORT, sections, STANDARD_DISCLAIMER,
                buildSignature(reportNumber, generatedAt),
                new Footer(GENERATED_BY, PLATFORM_VERSION, generatedAt, 0)
        );
    }

    public static ReportViewModel buildIntervention(String reportNumber, ReportMetadata metadata,
                                                     Instant periodStart, Instant periodEnd, Instant generatedAt) {
        String periodLabel = "%s au %s".formatted(DATE_FMT.format(periodStart), DATE_FMT.format(periodEnd));

        List<Section> sections = new ArrayList<>();

        sections.add(new Section("Résumé de l'intervention", SectionType.EXECUTIVE_SUMMARY,
                List.of(
                        new KeyValue("Bâtiment", metadata.buildingName(), Severity.INFO),
                        new KeyValue("Adresse", metadata.buildingAddress(), Severity.INFO),
                        new KeyValue("Score de risque", "%.0f / 100".formatted(metadata.averageRiskScore()),
                                classifySeverity(metadata.averageRiskScore()))
                ), List.of(), List.of(), null));

        sections.add(new Section("Indicateurs d'intervention", SectionType.INTERVENTION_SUMMARY,
                List.of(
                        new KeyValue("Interventions planifiées", String.valueOf(metadata.interventionCount()), Severity.INFO),
                        new KeyValue("Interventions réalisées", String.valueOf(metadata.resolvedInterventionCount()),
                                metadata.resolvedInterventionCount() < metadata.interventionCount() ? Severity.WARNING : Severity.NORMAL),
                        new KeyValue("Taux de complétion", metadata.interventionCount() > 0
                                ? "%.0f%%".formatted(metadata.resolvedInterventionCount() * 100.0 / metadata.interventionCount())
                                : "N/A", Severity.INFO),
                        new KeyValue("Défauts confirmés", String.valueOf(metadata.confirmedDefects()),
                                metadata.confirmedDefects() > 0 ? Severity.WARNING : Severity.NORMAL),
                        new KeyValue("Faux positifs", String.valueOf(metadata.falsePositives()), Severity.INFO)
                ), List.of(), List.of(), null));

        sections.add(new Section("Impact sur le risque", SectionType.RISK_ANALYSIS,
                List.of(
                        new KeyValue("Évolution du risque après intervention", "%.1f%%".formatted(metadata.riskEvolutionPercent()),
                                metadata.riskEvolutionPercent() < 0 ? Severity.NORMAL : Severity.WARNING)
                ), List.of(), List.of(),
                metadata.riskEvolutionPercent() < 0
                        ? "L'intervention a contribué à une réduction du score de risque global."
                        : "Le score de risque n'a pas diminué après l'intervention. Un suivi rapproché est recommandé."));

        if (metadata.recommendations() != null && !metadata.recommendations().isEmpty()) {
            sections.add(new Section("Recommandations post-intervention", SectionType.RECOMMENDATIONS,
                    List.of(), List.of(), metadata.recommendations(), null));
        }

        return new ReportViewModel(
                new Header("Rapport d'Intervention Préventive", reportNumber,
                        metadata.buildingName(), metadata.buildingName(), metadata.buildingAddress(),
                        periodLabel, generatedAt, classifyRisk(metadata.averageRiskScore())),
                ReportType.INTERVENTION_REPORT, sections, STANDARD_DISCLAIMER,
                buildSignature(reportNumber, generatedAt),
                new Footer(GENERATED_BY, PLATFORM_VERSION, generatedAt, 0)
        );
    }

    private static Section buildExecutiveSummary(ReportMetadata metadata, String periodLabel) {
        RiskLevel risk = classifyRisk(metadata.averageRiskScore());
        String narrative = "Durant la période du %s, le système PyroSense AI a assuré la surveillance ".formatted(periodLabel) +
                "de %d capteurs sur le bâtiment « %s ». ".formatted(metadata.sensorCount(), metadata.buildingName()) +
                "Le score de risque moyen est de %.1f/100, classé %s.".formatted(metadata.averageRiskScore(), risk.name());
        return new Section("Synthèse exécutive", SectionType.EXECUTIVE_SUMMARY,
                List.of(), List.of(), List.of(), narrative);
    }

    private static Section buildKpiSection(ReportMetadata metadata) {
        int totalSensors = metadata.sensorCount() + metadata.offlineSensorCount();
        double availability = totalSensors > 0 ? (metadata.sensorCount() * 100.0 / totalSensors) : 100.0;
        return new Section("Indicateurs clés", SectionType.KPI_INDICATORS,
                List.of(
                        new KeyValue("Capteurs actifs", "%d / %d".formatted(metadata.sensorCount(), totalSensors),
                                metadata.offlineSensorCount() > 0 ? Severity.WARNING : Severity.NORMAL),
                        new KeyValue("Disponibilité", "%.1f%%".formatted(availability),
                                availability < 95 ? Severity.WARNING : Severity.NORMAL),
                        new KeyValue("Score de risque moyen", "%.1f / 100".formatted(metadata.averageRiskScore()),
                                classifySeverity(metadata.averageRiskScore())),
                        new KeyValue("Évolution du risque", "%+.1f%%".formatted(metadata.riskEvolutionPercent()),
                                metadata.riskEvolutionPercent() > 5 ? Severity.WARNING : Severity.NORMAL)
                ), List.of(), List.of(), null);
    }

    private static Section buildRiskAnalysisSection(ReportMetadata metadata) {
        List<String> topRisk = metadata.topRiskBuildings() != null ? metadata.topRiskBuildings() : List.of();
        return new Section("Analyse des risques", SectionType.RISK_ANALYSIS,
                List.of(
                        new KeyValue("Niveau global", classifyRisk(metadata.averageRiskScore()).name(),
                                classifySeverity(metadata.averageRiskScore()))
                ), List.of(), topRisk.isEmpty() ? List.of() : topRisk, null);
    }

    private static Section buildAlertSection(ReportMetadata metadata) {
        return new Section("Bilan des alertes", SectionType.ALERT_SUMMARY,
                List.of(
                        new KeyValue("Alertes totales", String.valueOf(metadata.alertCount()), Severity.INFO),
                        new KeyValue("Alertes critiques", String.valueOf(metadata.criticalAlertCount()),
                                metadata.criticalAlertCount() > 0 ? Severity.CRITICAL : Severity.NORMAL),
                        new KeyValue("Défauts confirmés", String.valueOf(metadata.confirmedDefects()),
                                metadata.confirmedDefects() > 0 ? Severity.WARNING : Severity.NORMAL),
                        new KeyValue("Faux positifs", String.valueOf(metadata.falsePositives()), Severity.INFO)
                ), List.of(), List.of(), null);
    }

    private static Section buildInterventionSection(ReportMetadata metadata) {
        return new Section("Bilan des interventions", SectionType.INTERVENTION_SUMMARY,
                List.of(
                        new KeyValue("Interventions créées", String.valueOf(metadata.interventionCount()), Severity.INFO),
                        new KeyValue("Interventions complétées", String.valueOf(metadata.resolvedInterventionCount()),
                                metadata.resolvedInterventionCount() < metadata.interventionCount() ? Severity.WARNING : Severity.NORMAL),
                        new KeyValue("Taux de résolution", metadata.interventionCount() > 0
                                ? "%.0f%%".formatted(metadata.resolvedInterventionCount() * 100.0 / metadata.interventionCount())
                                : "N/A", Severity.INFO)
                ), List.of(), List.of(), null);
    }

    private static Section buildRecommendationsSection(ReportMetadata metadata) {
        List<String> recos = metadata.recommendations() != null ? metadata.recommendations() : List.of();
        String roiNarrative = metadata.roiSummary() != null
                ? "Synthèse ROI : " + metadata.roiSummary()
                : null;
        return new Section("Recommandations", SectionType.RECOMMENDATIONS,
                List.of(), List.of(), recos, roiNarrative);
    }

    private static Signature buildSignature(String reportNumber, Instant generatedAt) {
        return new Signature(null, "SHA-256", reportNumber, generatedAt);
    }

    static RiskLevel classifyRisk(double score) {
        if (score >= 75) return RiskLevel.CRITICAL;
        if (score >= 50) return RiskLevel.HIGH;
        if (score >= 25) return RiskLevel.MODERATE;
        return RiskLevel.LOW;
    }

    static Severity classifySeverity(double score) {
        if (score >= 75) return Severity.CRITICAL;
        if (score >= 50) return Severity.WARNING;
        return Severity.NORMAL;
    }
}
