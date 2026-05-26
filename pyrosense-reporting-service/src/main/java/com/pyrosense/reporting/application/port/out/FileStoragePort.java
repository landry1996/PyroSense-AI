package com.pyrosense.reporting.application.port.out;

import com.pyrosense.reporting.domain.model.ReportFileReference;

import java.util.UUID;

public interface FileStoragePort {

    ReportFileReference store(UUID reportId, String fileName, byte[] content);

    byte[] retrieve(ReportFileReference reference);

    void delete(ReportFileReference reference);
}
