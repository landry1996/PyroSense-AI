package com.pyrosense.reporting.adapter.out.renderer;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.pyrosense.reporting.application.port.out.PdfRendererPort;
import com.pyrosense.reporting.domain.model.ReportViewModel;
import com.pyrosense.reporting.domain.model.ReportViewModel.Footer;
import com.pyrosense.reporting.domain.model.ReportViewModel.KeyValue;
import com.pyrosense.reporting.domain.model.ReportViewModel.LegalDisclaimer;
import com.pyrosense.reporting.domain.model.ReportViewModel.RiskLevel;
import com.pyrosense.reporting.domain.model.ReportViewModel.Severity;
import com.pyrosense.reporting.domain.model.ReportViewModel.Signature;
import com.pyrosense.reporting.domain.model.ReportViewModel.TableData;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class ProfessionalPdfRenderer implements PdfRendererPort {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneOffset.UTC);

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static final Font SMALL_ITALIC = new Font(Font.HELVETICA, 8, Font.ITALIC);
    private static final Font SMALL_BOLD = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font DISCLAIMER_FONT = new Font(Font.HELVETICA, 7, Font.ITALIC);

    private static final Color HEADER_BG = new Color(45, 55, 72);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(200, 200, 200);
    private static final Color CRITICAL_COLOR = new Color(180, 30, 30);
    private static final Color WARNING_COLOR = new Color(180, 120, 0);
    private static final Color NORMAL_COLOR = new Color(30, 130, 60);

    @Override
    public byte[] render(ReportViewModel viewModel) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 50, 60);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new FooterPageEvent(viewModel.footer(), viewModel.header().reportNumber()));
            document.open();

            renderHeader(document, viewModel.header());
            renderSections(document, viewModel.sections());
            renderDisclaimer(document, viewModel.disclaimer());
            renderSignatureBlock(document, viewModel.signature());

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Professional PDF rendering failed", e);
        }
    }

    private void renderHeader(Document document, ReportViewModel.Header header) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(15);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(HEADER_BG);
        titleCell.setPadding(12);
        titleCell.setBorder(Rectangle.NO_BORDER);

        Paragraph titlePara = new Paragraph();
        titlePara.add(new Chunk("PYROSENSE AI", new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE)));
        titlePara.add(Chunk.NEWLINE);
        titlePara.add(new Chunk(header.reportTitle().toUpperCase(), new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE)));
        titleCell.addElement(titlePara);
        headerTable.addCell(titleCell);
        document.add(headerTable);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{50, 50});
        infoTable.setSpacingAfter(5);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(5);
        Paragraph leftInfo = new Paragraph();
        leftInfo.add(new Chunk("Rapport N° ", LABEL_FONT));
        leftInfo.add(new Chunk(header.reportNumber(), BODY_FONT));
        leftInfo.add(Chunk.NEWLINE);
        leftInfo.add(new Chunk("Période : ", LABEL_FONT));
        leftInfo.add(new Chunk(header.periodLabel(), BODY_FONT));
        leftCell.addElement(leftInfo);
        infoTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(5);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph rightInfo = new Paragraph();
        rightInfo.setAlignment(Element.ALIGN_RIGHT);
        rightInfo.add(new Chunk("Bâtiment : ", LABEL_FONT));
        rightInfo.add(new Chunk(header.buildingName(), BODY_FONT));
        rightInfo.add(Chunk.NEWLINE);
        rightInfo.add(new Chunk("Adresse : ", LABEL_FONT));
        rightInfo.add(new Chunk(header.buildingAddress() != null ? header.buildingAddress() : "—", BODY_FONT));
        rightCell.addElement(rightInfo);
        infoTable.addCell(rightCell);
        document.add(infoTable);

        if (header.overallRiskLevel() != null) {
            PdfPTable riskBadge = new PdfPTable(1);
            riskBadge.setWidthPercentage(30);
            riskBadge.setHorizontalAlignment(Element.ALIGN_RIGHT);
            riskBadge.setSpacingAfter(10);

            PdfPCell badgeCell = new PdfPCell(new Phrase("Niveau de risque : " + header.overallRiskLevel().name(),
                    new Font(Font.HELVETICA, 9, Font.BOLD, riskColor(header.overallRiskLevel()))));
            badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeCell.setPadding(5);
            badgeCell.setBorderColor(riskColor(header.overallRiskLevel()));
            badgeCell.setBorderWidth(1.5f);
            riskBadge.addCell(badgeCell);
            document.add(riskBadge);
        }

        document.add(createSeparator());
    }

    private void renderSections(Document document, java.util.List<ReportViewModel.Section> sections) throws DocumentException {
        for (ReportViewModel.Section section : sections) {
            renderSection(document, section);
        }
    }

    private void renderSection(Document document, ReportViewModel.Section section) throws DocumentException {
        Paragraph sectionTitle = new Paragraph(section.title(), SECTION_FONT);
        sectionTitle.setSpacingBefore(12);
        sectionTitle.setSpacingAfter(6);
        document.add(sectionTitle);

        document.add(createThinSeparator());

        if (section.narrative() != null && !section.narrative().isEmpty()) {
            Paragraph narrative = new Paragraph(section.narrative(), BODY_FONT);
            narrative.setSpacingAfter(8);
            narrative.setLeading(13);
            document.add(narrative);
        }

        if (!section.indicators().isEmpty()) {
            renderIndicators(document, section.indicators());
        }

        if (!section.tables().isEmpty()) {
            for (TableData table : section.tables()) {
                renderTable(document, table);
            }
        }

        if (!section.bulletPoints().isEmpty()) {
            renderBulletPoints(document, section.bulletPoints());
        }
    }

    private void renderIndicators(Document document, java.util.List<KeyValue> indicators) throws DocumentException {
        int cols = Math.min(indicators.size(), 3);
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(8);

        for (KeyValue kv : indicators) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setBackgroundColor(LIGHT_GRAY);
            cell.setPadding(8);

            Paragraph content = new Paragraph();
            content.add(new Chunk(kv.label(), SMALL_FONT));
            content.add(Chunk.NEWLINE);
            Font valueFont = new Font(Font.HELVETICA, 11, Font.BOLD, severityColor(kv.severity()));
            content.add(new Chunk(kv.value(), valueFont));
            cell.addElement(content);
            table.addCell(cell);
        }

        int remainder = cols - (indicators.size() % cols);
        if (remainder != cols) {
            for (int i = 0; i < remainder; i++) {
                PdfPCell empty = new PdfPCell();
                empty.setBorder(Rectangle.NO_BORDER);
                table.addCell(empty);
            }
        }

        document.add(table);
    }

    private void renderTable(Document document, TableData tableData) throws DocumentException {
        if (tableData.title() != null) {
            Paragraph tableTitle = new Paragraph(tableData.title(), SUBTITLE_FONT);
            tableTitle.setSpacingBefore(5);
            tableTitle.setSpacingAfter(3);
            document.add(tableTitle);
        }

        int colCount = tableData.columnHeaders().size();
        PdfPTable table = new PdfPTable(colCount);
        table.setWidthPercentage(100);
        table.setSpacingAfter(8);

        for (String header : tableData.columnHeaders()) {
            PdfPCell cell = new PdfPCell(new Phrase(header, SMALL_BOLD));
            cell.setBackgroundColor(LIGHT_GRAY);
            cell.setPadding(5);
            cell.setBorderColor(BORDER_COLOR);
            cell.setBorderWidth(0.5f);
            table.addCell(cell);
        }

        for (java.util.List<String> row : tableData.rows()) {
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(value, SMALL_FONT));
                cell.setPadding(4);
                cell.setBorderColor(BORDER_COLOR);
                cell.setBorderWidth(0.5f);
                table.addCell(cell);
            }
        }

        document.add(table);
    }

    private void renderBulletPoints(Document document, java.util.List<String> points) throws DocumentException {
        for (String point : points) {
            Paragraph bullet = new Paragraph("  •  " + point, BODY_FONT);
            bullet.setIndentationLeft(10);
            bullet.setSpacingAfter(3);
            document.add(bullet);
        }
    }

    private void renderDisclaimer(Document document, LegalDisclaimer disclaimer) throws DocumentException {
        document.add(createSeparator());

        Paragraph title = new Paragraph("Limites et avertissements", SMALL_BOLD);
        title.setSpacingBefore(10);
        title.setSpacingAfter(4);
        document.add(title);

        Paragraph mainText = new Paragraph(disclaimer.mainText(), DISCLAIMER_FONT);
        mainText.setLeading(10);
        mainText.setSpacingAfter(4);
        document.add(mainText);

        for (String limitation : disclaimer.limitations()) {
            Paragraph lim = new Paragraph("  —  " + limitation, DISCLAIMER_FONT);
            lim.setIndentationLeft(8);
            lim.setSpacingAfter(2);
            document.add(lim);
        }
    }

    private void renderSignatureBlock(Document document, Signature signature) throws DocumentException {
        Paragraph sigBlock = new Paragraph();
        sigBlock.setSpacingBefore(15);
        sigBlock.add(new Chunk("Signature logique : ", SMALL_BOLD));
        sigBlock.add(new Chunk("Rapport N° %s — Algorithme %s — %s".formatted(
                signature.reportNumber(), signature.algorithm(),
                DATETIME_FMT.format(signature.timestamp())), SMALL_ITALIC));
        document.add(sigBlock);

        Paragraph validity = new Paragraph(
                "L'intégrité de ce document peut être vérifiée via le hash SHA-256 du contenu PDF.",
                DISCLAIMER_FONT);
        validity.setSpacingBefore(2);
        document.add(validity);
    }

    private Paragraph createSeparator() {
        Paragraph sep = new Paragraph();
        sep.setSpacingBefore(5);
        sep.setSpacingAfter(5);
        LineSeparator line = new LineSeparator(0.5f, 100, BORDER_COLOR, Element.ALIGN_CENTER, -2);
        sep.add(line);
        return sep;
    }

    private Paragraph createThinSeparator() {
        Paragraph sep = new Paragraph();
        sep.setSpacingAfter(4);
        LineSeparator line = new LineSeparator(0.3f, 100, BORDER_COLOR, Element.ALIGN_CENTER, -2);
        sep.add(line);
        return sep;
    }

    private Color riskColor(RiskLevel level) {
        return switch (level) {
            case CRITICAL -> CRITICAL_COLOR;
            case HIGH -> WARNING_COLOR;
            case MODERATE -> new Color(100, 100, 100);
            case LOW -> NORMAL_COLOR;
        };
    }

    private Color severityColor(Severity severity) {
        return switch (severity) {
            case CRITICAL -> CRITICAL_COLOR;
            case WARNING -> WARNING_COLOR;
            case NORMAL -> NORMAL_COLOR;
            case INFO -> Color.BLACK;
        };
    }

    private static class FooterPageEvent extends PdfPageEventHelper {
        private final Footer footer;
        private final String reportNumber;

        FooterPageEvent(Footer footer, String reportNumber) {
            this.footer = footer;
            this.reportNumber = reportNumber;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float pageWidth = document.right() - document.left();
            float y = document.bottom() - 25;

            Font footerFont = new Font(Font.HELVETICA, 7, Font.NORMAL, new Color(120, 120, 120));

            String leftText = "%s v%s — %s".formatted(
                    footer.generatedBy(), footer.version(),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC).format(footer.timestamp()));
            String rightText = "N° %s — Page %d".formatted(reportNumber, writer.getPageNumber());

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(leftText, footerFont),
                    document.left(), y, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(rightText, footerFont),
                    document.right(), y, 0);

            cb.setLineWidth(0.3f);
            cb.setColorStroke(new Color(200, 200, 200));
            cb.moveTo(document.left(), y + 10);
            cb.lineTo(document.right(), y + 10);
            cb.stroke();
        }
    }
}
