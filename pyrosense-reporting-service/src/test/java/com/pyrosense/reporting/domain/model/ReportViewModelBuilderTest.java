package com.pyrosense.reporting.domain.model;

import com.pyrosense.reporting.domain.model.ReportViewModel.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ReportViewModelBuilderTest {

    private static final Instant START = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2025-01-31T23:59:59Z");
    private static final Instant NOW = Instant.parse("2025-02-01T10:00:00Z");

    @Test
    void monthlyHealthShouldHaveCorrectType() {
        ReportViewModel vm = ReportViewModelBuilder.buildMonthlyHealth("MH-001", metadata(), START, END, NOW);
        assertThat(vm.type()).isEqualTo(ReportType.MONTHLY_HEALTH);
    }

    @Test
    void certificateShouldHaveCorrectType() {
        ReportViewModel vm = ReportViewModelBuilder.buildMonitoringCertificate("CM-001", metadata(), START, END, NOW);
        assertThat(vm.type()).isEqualTo(ReportType.CONTINUOUS_MONITORING_CERTIFICATE);
    }

    @Test
    void criticalAlertShouldHaveCorrectType() {
        ReportViewModel vm = ReportViewModelBuilder.buildCriticalAlert("CA-001", metadata(), START, END, NOW);
        assertThat(vm.type()).isEqualTo(ReportType.CRITICAL_ALERT_REPORT);
    }

    @Test
    void interventionShouldHaveCorrectType() {
        ReportViewModel vm = ReportViewModelBuilder.buildIntervention("IR-001", metadata(), START, END, NOW);
        assertThat(vm.type()).isEqualTo(ReportType.INTERVENTION_REPORT);
    }

    @Test
    void shouldClassifyRiskLevelsCorrectly() {
        assertThat(ReportViewModelBuilder.classifyRisk(10)).isEqualTo(RiskLevel.LOW);
        assertThat(ReportViewModelBuilder.classifyRisk(30)).isEqualTo(RiskLevel.MODERATE);
        assertThat(ReportViewModelBuilder.classifyRisk(55)).isEqualTo(RiskLevel.HIGH);
        assertThat(ReportViewModelBuilder.classifyRisk(80)).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void headerShouldContainReportNumber() {
        ReportViewModel vm = ReportViewModelBuilder.buildMonthlyHealth("MH-202501-00042", metadata(), START, END, NOW);
        assertThat(vm.header().reportNumber()).isEqualTo("MH-202501-00042");
    }

    @Test
    void headerShouldContainPeriodLabel() {
        ReportViewModel vm = ReportViewModelBuilder.buildMonthlyHealth("MH-001", metadata(), START, END, NOW);
        assertThat(vm.header().periodLabel()).contains("01/01/2025");
        assertThat(vm.header().periodLabel()).contains("31/01/2025");
    }

    @Test
    void monthlyHealthShouldHaveKpiIndicators() {
        ReportViewModel vm = ReportViewModelBuilder.buildMonthlyHealth("MH-001", metadata(), START, END, NOW);
        var kpiSection = vm.sections().stream()
                .filter(s -> s.sectionType() == SectionType.KPI_INDICATORS)
                .findFirst().orElseThrow();
        assertThat(kpiSection.indicators()).isNotEmpty();
        assertThat(kpiSection.indicators()).extracting("label")
                .contains("Capteurs actifs", "Score de risque moyen");
    }

    @Test
    void criticalAlertShouldHaveUrgentRecommendations() {
        ReportViewModel vm = ReportViewModelBuilder.buildCriticalAlert("CA-001", highRiskMetadata(), START, END, NOW);
        var recoSection = vm.sections().stream()
                .filter(s -> s.sectionType() == SectionType.RECOMMENDATIONS)
                .findFirst().orElseThrow();
        assertThat(recoSection.bulletPoints()).isNotEmpty();
        assertThat(recoSection.bulletPoints().get(0)).contains("Inspection");
    }

    @Test
    void certificateShouldIncludeAvailabilityRate() {
        ReportViewModel vm = ReportViewModelBuilder.buildMonitoringCertificate("CM-001", metadata(), START, END, NOW);
        var paramSection = vm.sections().stream()
                .filter(s -> s.title().equals("Paramètres de surveillance"))
                .findFirst().orElseThrow();
        assertThat(paramSection.indicators()).extracting("label")
                .contains("Taux de disponibilité");
    }

    @Test
    void interventionShouldShowCompletionRate() {
        ReportViewModel vm = ReportViewModelBuilder.buildIntervention("IR-001", metadata(), START, END, NOW);
        var interventionSection = vm.sections().stream()
                .filter(s -> s.sectionType() == SectionType.INTERVENTION_SUMMARY)
                .findFirst().orElseThrow();
        assertThat(interventionSection.indicators()).extracting("label")
                .contains("Taux de complétion");
    }

    @Test
    void highRiskShouldUseCriticalSeverity() {
        assertThat(ReportViewModelBuilder.classifySeverity(80)).isEqualTo(Severity.CRITICAL);
        assertThat(ReportViewModelBuilder.classifySeverity(60)).isEqualTo(Severity.WARNING);
        assertThat(ReportViewModelBuilder.classifySeverity(20)).isEqualTo(Severity.NORMAL);
    }

    private static ReportMetadata metadata() {
        return new ReportMetadata(
                "Bâtiment A", "123 Rue Test, 75001 Paris",
                12, 2, 42.0, 8, 2, 5, 4, 3, 1, -3.0,
                List.of("Bâtiment A — Score 42"),
                List.of("Vérifier câblage", "Planifier maintenance"),
                "2 incidents évités"
        );
    }

    private static ReportMetadata highRiskMetadata() {
        return new ReportMetadata(
                "Tour B", "456 Avenue Danger, 75009 Paris",
                20, 0, 85.0, 15, 10, 3, 1, 2, 0, +15.0,
                List.of("Tour B — Score 85"),
                List.of("Coupure préventive recommandée"),
                null
        );
    }
}
