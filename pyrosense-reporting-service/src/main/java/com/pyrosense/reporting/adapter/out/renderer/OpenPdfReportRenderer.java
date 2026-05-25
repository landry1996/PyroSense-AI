package com.pyrosense.reporting.adapter.out.renderer;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pyrosense.reporting.application.port.out.ReportRendererPort;
import com.pyrosense.reporting.domain.model.ReportMetadata;
import com.pyrosense.reporting.domain.model.ReportType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class OpenPdfReportRenderer implements ReportRendererPort {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneOffset.UTC);
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);

    @Override
    public byte[] render(ReportRenderRequest request) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, request);
            addSummarySection(document, request.metadata());
            addTypeSpecificContent(document, request);
            addFooter(document, request);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF rendering failed", e);
        }
    }

    private void addHeader(Document document, ReportRenderRequest request) throws DocumentException {
        Paragraph title = new Paragraph("PyroSense - " + formatReportTitle(request.type()), TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Paragraph subtitle = new Paragraph("Report N° " + request.reportNumber(), HEADER_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(5);
        document.add(subtitle);

        Paragraph period = new Paragraph(
                "Période : %s - %s".formatted(DATE_FMT.format(request.periodStart()), DATE_FMT.format(request.periodEnd())),
                BODY_FONT);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(5);
        document.add(period);

        Paragraph building = new Paragraph("Bâtiment : " + request.tenantName(), BODY_FONT);
        building.setAlignment(Element.ALIGN_CENTER);
        building.setSpacingAfter(20);
        document.add(building);

        document.add(new Paragraph(" "));
    }

    private void addSummarySection(Document document, ReportMetadata metadata) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Résumé", HEADER_FONT);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40, 60});

        addTableRow(table, "Bâtiment", metadata.buildingName());
        addTableRow(table, "Adresse", metadata.buildingAddress());
        addTableRow(table, "Capteurs actifs", String.valueOf(metadata.sensorCount()));
        addTableRow(table, "Score de risque moyen", "%.1f / 100".formatted(metadata.averageRiskScore()));
        addTableRow(table, "Alertes totales", String.valueOf(metadata.alertCount()));
        addTableRow(table, "Alertes critiques", String.valueOf(metadata.criticalAlertCount()));
        addTableRow(table, "Interventions", String.valueOf(metadata.interventionCount()));
        addTableRow(table, "Interventions résolues", String.valueOf(metadata.resolvedInterventionCount()));

        document.add(table);
    }

    private void addTypeSpecificContent(Document document, ReportRenderRequest request) throws DocumentException {
        ReportMetadata metadata = request.metadata();
        document.add(new Paragraph(" "));

        switch (request.type()) {
            case MONTHLY_HEALTH -> addMonthlyHealthContent(document, metadata);
            case CONTINUOUS_MONITORING_CERTIFICATE -> addCertificateContent(document, request);
            case CRITICAL_ALERT_REPORT -> addCriticalAlertContent(document, metadata);
            case INTERVENTION_REPORT -> addInterventionContent(document, metadata);
            case ROI_AVOIDED_INCIDENTS -> addRoiContent(document, metadata);
            case INSURER_EXPORT -> addInsurerContent(document, metadata, request);
        }
    }

    private void addMonthlyHealthContent(Document document, ReportMetadata metadata) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Bilan Mensuel de Santé Électrique", HEADER_FONT);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        if (metadata.recommendations() != null && !metadata.recommendations().isEmpty()) {
            Paragraph recoTitle = new Paragraph("Recommandations :", HEADER_FONT);
            recoTitle.setSpacingBefore(10);
            document.add(recoTitle);
            for (String reco : metadata.recommendations()) {
                document.add(new Paragraph("• " + reco, BODY_FONT));
            }
        }
    }

    private void addCertificateContent(Document document, ReportRenderRequest request) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Attestation de Surveillance Continue", HEADER_FONT);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        document.add(new Paragraph(
                "Ce document certifie que le bâtiment \"%s\" a été sous surveillance électrique continue "
                        .formatted(request.tenantName())
                        + "pendant la période du %s au %s.".formatted(
                        DATE_FMT.format(request.periodStart()), DATE_FMT.format(request.periodEnd())),
                BODY_FONT));
    }

    private void addCriticalAlertContent(Document document, ReportMetadata metadata) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Rapport des Alertes Critiques", HEADER_FONT);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        document.add(new Paragraph(
                "Nombre d'alertes critiques détectées : %d".formatted(metadata.criticalAlertCount()), BODY_FONT));
    }

    private void addInterventionContent(Document document, ReportMetadata metadata) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Rapport d'Interventions", HEADER_FONT);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        document.add(new Paragraph(
                "Interventions réalisées : %d / %d".formatted(
                        metadata.resolvedInterventionCount(), metadata.interventionCount()), BODY_FONT));
    }

    private void addRoiContent(Document document, ReportMetadata metadata) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("ROI - Incidents Évités", HEADER_FONT);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        document.add(new Paragraph(
                "Grâce à la surveillance continue et aux interventions préventives, "
                        + "%d interventions ont permis d'éviter des incidents potentiels.".formatted(
                        metadata.resolvedInterventionCount()), BODY_FONT));
    }

    private void addInsurerContent(Document document, ReportMetadata metadata, ReportRenderRequest request) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Export Assureur", HEADER_FONT);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        document.add(new Paragraph("Données de conformité pour la période %s - %s".formatted(
                DATE_FMT.format(request.periodStart()), DATE_FMT.format(request.periodEnd())), BODY_FONT));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        addTableRow(table, "Score de risque moyen", "%.1f".formatted(metadata.averageRiskScore()));
        addTableRow(table, "Alertes critiques", String.valueOf(metadata.criticalAlertCount()));
        addTableRow(table, "Interventions résolues", String.valueOf(metadata.resolvedInterventionCount()));
        document.add(table);
    }

    private void addFooter(Document document, ReportRenderRequest request) throws DocumentException {
        document.add(new Paragraph(" "));
        Paragraph footer = new Paragraph(
                "Document généré automatiquement par PyroSense AI Platform - %s".formatted(
                        DATE_FMT.format(Instant.now())),
                new Font(Font.HELVETICA, 8, Font.ITALIC));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BODY_FONT));
        labelCell.setBorderWidth(0.5f);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, BODY_FONT));
        valueCell.setBorderWidth(0.5f);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private String formatReportTitle(ReportType type) {
        return switch (type) {
            case MONTHLY_HEALTH -> "Bilan Mensuel de Santé Électrique";
            case CONTINUOUS_MONITORING_CERTIFICATE -> "Attestation de Surveillance Continue";
            case CRITICAL_ALERT_REPORT -> "Rapport des Alertes Critiques";
            case INTERVENTION_REPORT -> "Rapport d'Interventions";
            case ROI_AVOIDED_INCIDENTS -> "ROI - Incidents Évités";
            case INSURER_EXPORT -> "Export Assureur";
        };
    }
}
