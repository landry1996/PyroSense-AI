package com.pyrosense.ingestion.application.port.out;

import java.time.Duration;

public interface IdempotencyPort {

    boolean isDuplicate(String key);

    void markProcessed(String key, Duration ttl);
}
