package com.pyrosense.reporting.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ReportRecipient(UUID userId, String name, String role) {
    public ReportRecipient {
        Objects.requireNonNull(userId, "userId must not be null");
    }
}
