package com.pyrosense.notification.application.port.out;

import com.pyrosense.notification.domain.model.DeduplicationKey;

public interface DeduplicationPort {
    boolean isDuplicate(DeduplicationKey key);
    void markSent(DeduplicationKey key);
}
