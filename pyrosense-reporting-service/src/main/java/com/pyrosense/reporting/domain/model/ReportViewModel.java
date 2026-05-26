package com.pyrosense.reporting.domain.model;

import java.time.Instant;
import java.util.List;

public record ReportViewModel(
        Header header,
        ReportType type,
        List<Section> sections,
        LegalDisclaimer disclaimer,
        Signature signature,
        Footer footer
) {

    public record Header(
            String reportTitle,
            String reportNumber,
            String tenantName,
            String buildingName,
            String buildingAddress,
            String periodLabel,
            Instant generatedAt,
            RiskLevel overallRiskLevel
    ) {}

    public record Section(
            String title,
            SectionType sectionType,
            List<KeyValue> indicators,
            List<TableData> tables,
            List<String> bulletPoints,
            String narrative
    ) {}

    public record KeyValue(String label, String value, Severity severity) {}

    public record TableData(
            String title,
            List<String> columnHeaders,
            List<List<String>> rows
    ) {}

    public record LegalDisclaimer(
            String mainText,
            List<String> limitations
    ) {}

    public record Signature(
            String hash,
            String algorithm,
            String reportNumber,
            Instant timestamp
    ) {}

    public record Footer(
            String generatedBy,
            String version,
            Instant timestamp,
            int pageCount
    ) {}

    public enum SectionType {
        EXECUTIVE_SUMMARY,
        KPI_INDICATORS,
        RISK_ANALYSIS,
        ALERT_SUMMARY,
        INTERVENTION_SUMMARY,
        RECOMMENDATIONS,
        CERTIFICATION,
        DIAGNOSTIC,
        TIMELINE,
        ROI_ANALYSIS
    }

    public enum RiskLevel {
        LOW, MODERATE, HIGH, CRITICAL
    }

    public enum Severity {
        NORMAL, WARNING, CRITICAL, INFO
    }
}
