package com.pyrosense.reporting.domain.model;

import java.util.Objects;

public record ReportFileReference(String storagePath, String bucket, long sizeBytes) {
    public ReportFileReference {
        Objects.requireNonNull(storagePath, "storagePath must not be null");
    }
}
