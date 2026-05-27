package com.pyrosense.ingestion.application.port.out;

import java.util.UUID;

public interface DatasetAuditPort {

    void logFeedbackSubmitted(String tenantId, String technicianId, UUID interventionId, String labelValue);

    void logExportRequested(String tenantId, String requestedBy, UUID jobId, String format);

    void logExportCompleted(String tenantId, UUID jobId, int candidateCount);

    void logCandidateLabeled(String tenantId, UUID candidateId, String labelValue, String source);
}
