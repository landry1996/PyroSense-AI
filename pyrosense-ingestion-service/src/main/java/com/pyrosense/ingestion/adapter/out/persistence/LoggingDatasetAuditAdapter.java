package com.pyrosense.ingestion.adapter.out.persistence;

import com.pyrosense.ingestion.application.port.out.DatasetAuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LoggingDatasetAuditAdapter implements DatasetAuditPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingDatasetAuditAdapter.class);

    @Override
    public void logFeedbackSubmitted(String tenantId, String technicianId, UUID interventionId, String labelValue) {
        log.info("[AUDIT] FIELD_FEEDBACK_SUBMITTED tenant={} technician={} intervention={} label={}",
                tenantId, technicianId, interventionId, labelValue);
    }

    @Override
    public void logExportRequested(String tenantId, String requestedBy, UUID jobId, String format) {
        log.info("[AUDIT] DATASET_EXPORT_REQUESTED tenant={} requestedBy={} job={} format={}",
                tenantId, requestedBy, jobId, format);
    }

    @Override
    public void logExportCompleted(String tenantId, UUID jobId, int candidateCount) {
        log.info("[AUDIT] DATASET_EXPORT_COMPLETED tenant={} job={} candidates={}",
                tenantId, jobId, candidateCount);
    }

    @Override
    public void logCandidateLabeled(String tenantId, UUID candidateId, String labelValue, String source) {
        log.info("[AUDIT] CANDIDATE_LABELED tenant={} candidate={} label={} source={}",
                tenantId, candidateId, labelValue, source);
    }
}
