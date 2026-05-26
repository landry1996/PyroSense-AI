package com.pyrosense.reporting.adapter.out.renderer;

import com.pyrosense.reporting.domain.model.ReportMetadata;
import com.pyrosense.reporting.domain.model.ReportViewModel;
import com.pyrosense.reporting.domain.model.ReportViewModelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ProfessionalPdfRendererTest {

    private ProfessionalPdfRenderer renderer;
    private static final Instant PERIOD_START = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2025-01-31T23:59:59Z");
    private static final Instant GENERATED_AT = Instant.parse("2025-02-01T09:00:00Z");

    @BeforeEach
    void setUp() {
        renderer = new ProfessionalPdfRenderer();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allReportViewModels")
    void shouldRenderValidPdfForAllReportTypes(String name, ReportViewModel viewModel) {
        byte[] result = renderer.render(viewModel);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(500);
        assertThat((char) result[0]).isEqualTo('%');
        assertThat((char) result[1]).isEqualTo('P');
        assertThat((char) result[2]).isEqualTo('D');
        assertThat((char) result[3]).isEqualTo('F');
    }

    @Test
    void monthlyHealthShouldContainMandatorySections() {
        ReportViewModel vm = buildMonthlyHealth();

        assertThat(vm.sections()).extracting("title")
                .contains("Synthèse exécutive", "Indicateurs clés", "Analyse des risques",
                        "Bilan des alertes", "Bilan des interventions", "Recommandations");
    }

    @Test
    void monitoringCertificateShouldContainMandatorySections() {
        ReportViewModel vm = buildCertificate();

        assertThat(vm.sections()).extracting("title")
                .contains("Objet de l'attestation", "Paramètres de surveillance",
                        "Synthèse des détections", "Conclusion");
    }

    @Test
    void criticalAlertShouldContainMandatorySections() {
        ReportViewModel vm = buildCriticalAlert();

        assertThat(vm.sections()).extracting("title")
                .contains("Nature de l'alerte", "Contexte de détection",
                        "Évaluation du risque", "Actions recommandées");
    }

    @Test
    void interventionShouldContainMandatorySections() {
        ReportViewModel vm = buildIntervention();

        assertThat(vm.sections()).extracting("title")
                .contains("Résumé de l'intervention", "Indicateurs d'intervention",
                        "Impact sur le risque", "Recommandations post-intervention");
    }

    @Test
    void allReportsShouldContainDisclaimer() {
        List<ReportViewModel> viewModels = List.of(
                buildMonthlyHealth(), buildCertificate(), buildCriticalAlert(), buildIntervention());

        for (ReportViewModel vm : viewModels) {
            assertThat(vm.disclaimer()).isNotNull();
            assertThat(vm.disclaimer().mainText()).isNotEmpty();
            assertThat(vm.disclaimer().mainText()).contains("aide à la décision");
            assertThat(vm.disclaimer().limitations()).isNotEmpty();
        }
    }

    @Test
    void disclaimerShouldNotClaimAbsoluteGuarantee() {
        List<ReportViewModel> viewModels = List.of(
                buildMonthlyHealth(), buildCertificate(), buildCriticalAlert(), buildIntervention());

        for (ReportViewModel vm : viewModels) {
            String fullDisclaimer = vm.disclaimer().mainText() + " " +
                    String.join(" ", vm.disclaimer().limitations());
            assertThat(fullDisclaimer).doesNotContain("garantie absolue d'absence d'incendie");
            assertThat(fullDisclaimer).doesNotContain("garantit l'absence");
            assertThat(fullDisclaimer).contains("ne constitue en aucun cas");
        }
    }

    @Test
    void allReportsShouldContainSignature() {
        List<ReportViewModel> viewModels = List.of(
                buildMonthlyHealth(), buildCertificate(), buildCriticalAlert(), buildIntervention());

        for (ReportViewModel vm : viewModels) {
            assertThat(vm.signature()).isNotNull();
            assertThat(vm.signature().algorithm()).isEqualTo("SHA-256");
            assertThat(vm.signature().reportNumber()).isNotEmpty();
            assertThat(vm.signature().timestamp()).isNotNull();
        }
    }

    @Test
    void allReportsShouldContainFooter() {
        List<ReportViewModel> viewModels = List.of(
                buildMonthlyHealth(), buildCertificate(), buildCriticalAlert(), buildIntervention());

        for (ReportViewModel vm : viewModels) {
            assertThat(vm.footer()).isNotNull();
            assertThat(vm.footer().generatedBy()).isEqualTo("PyroSense AI Platform");
            assertThat(vm.footer().version()).isNotEmpty();
            assertThat(vm.footer().timestamp()).isNotNull();
        }
    }

    @Test
    void criticalAlertShouldHaveCriticalRiskLevel() {
        ReportViewModel vm = buildCriticalAlert();
        assertThat(vm.header().overallRiskLevel()).isEqualTo(ReportViewModel.RiskLevel.CRITICAL);
    }

    @Test
    void recommendationsShouldUseCarefulWording() {
        ReportViewModel vm = buildCriticalAlert();
        var recoSection = vm.sections().stream()
                .filter(s -> s.title().equals("Actions recommandées"))
                .findFirst().orElseThrow();

        assertThat(recoSection.narrative()).contains("préconisations d'inspection");
        assertThat(recoSection.narrative()).contains("professionnel qualifié");
    }

    @Test
    void shouldProduceDifferentPdfsForDifferentTypes() {
        byte[] monthly = renderer.render(buildMonthlyHealth());
        byte[] certificate = renderer.render(buildCertificate());
        byte[] alert = renderer.render(buildCriticalAlert());
        byte[] intervention = renderer.render(buildIntervention());

        assertThat(monthly.length).isNotEqualTo(certificate.length);
        assertThat(alert.length).isNotEqualTo(intervention.length);
    }

    @Test
    void certificateDisclaimerShouldMentionRegulations() {
        ReportViewModel vm = buildCertificate();
        assertThat(vm.disclaimer().mainText()).contains("décret");
    }

    static Stream<Arguments> allReportViewModels() {
        return Stream.of(
                Arguments.of("Monthly Health", buildMonthlyHealth()),
                Arguments.of("Monitoring Certificate", buildCertificate()),
                Arguments.of("Critical Alert", buildCriticalAlert()),
                Arguments.of("Intervention", buildIntervention())
        );
    }

    private static ReportViewModel buildMonthlyHealth() {
        return ReportViewModelBuilder.buildMonthlyHealth(
                "MH-202501-00001", createMetadata(), PERIOD_START, PERIOD_END, GENERATED_AT);
    }

    private static ReportViewModel buildCertificate() {
        return ReportViewModelBuilder.buildMonitoringCertificate(
                "CM-202501-00001", createMetadata(), PERIOD_START, PERIOD_END, GENERATED_AT);
    }

    private static ReportViewModel buildCriticalAlert() {
        return ReportViewModelBuilder.buildCriticalAlert(
                "CA-202501-00001", createHighRiskMetadata(), PERIOD_START, PERIOD_END, GENERATED_AT);
    }

    private static ReportViewModel buildIntervention() {
        return ReportViewModelBuilder.buildIntervention(
                "IR-202501-00001", createMetadata(), PERIOD_START, PERIOD_END, GENERATED_AT);
    }

    private static ReportMetadata createMetadata() {
        return new ReportMetadata(
                "Résidence Les Acacias", "45 Avenue de la République, 75011 Paris",
                15, 2, 42.5, 12, 3, 7, 5, 4, 1, -3.5,
                List.of("Bâtiment B — Score 72", "Bâtiment C — Score 58"),
                List.of("Vérifier le circuit B3 (points chauds détectés)",
                        "Planifier resserrage connectique tableau principal",
                        "Remplacer capteur #7 (hors ligne depuis 48h)"),
                "3 incidents évités, économie estimée 45 000 €"
        );
    }

    private static ReportMetadata createHighRiskMetadata() {
        return new ReportMetadata(
                "Tour Haussmann", "12 Boulevard Haussmann, 75009 Paris",
                20, 0, 82.0, 15, 8, 4, 2, 3, 0, +12.5,
                List.of("Tour Haussmann — Score 82"),
                List.of("Inspection immédiate requise — micro-arcs récurrents",
                        "Coupure préventive recommandée sur circuit C7"),
                null
        );
    }
}
