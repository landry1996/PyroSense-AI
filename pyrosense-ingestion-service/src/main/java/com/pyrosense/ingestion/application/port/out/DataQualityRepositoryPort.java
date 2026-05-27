package com.pyrosense.ingestion.application.port.out;

import com.pyrosense.ingestion.domain.model.quality.DataQualityAssessment;
import com.pyrosense.ingestion.domain.model.quality.DataQualityIssue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataQualityRepositoryPort {

    void saveAssessment(DataQualityAssessment assessment);

    Optional<DataQualityAssessment> findLatestByDevice(String deviceId);

    Optional<DataQualityAssessment> findLatestByDeviceAndTenant(String deviceId, String tenantId);

    List<DataQualityAssessment> findHistoryByDevice(String deviceId, Instant from, Instant to);

    List<DataQualityAssessment> findHistoryByDeviceAndTenant(String deviceId, String tenantId, Instant from, Instant to);

    void saveIssue(DataQualityIssue issue);

    void saveIssues(List<DataQualityIssue> issues);

    Optional<DataQualityIssue> findIssueById(UUID id);

    List<DataQualityIssue> findIssues(String tenantId, String deviceId,
                                       DataQualityIssue.IssueStatus status,
                                       DataQualityIssue.IssueType type,
                                       int offset, int limit);

    int countIssues(String tenantId, String deviceId,
                    DataQualityIssue.IssueStatus status,
                    DataQualityIssue.IssueType type);

    void updateIssue(DataQualityIssue issue);
}
