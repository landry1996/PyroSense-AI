package com.pyrosense.notification.adapter.out.deduplication;

import com.pyrosense.notification.application.port.out.DeduplicationPort;
import com.pyrosense.notification.domain.model.DeduplicationKey;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemoryDeduplicationAdapter implements DeduplicationPort {

    private static final Duration DEDUP_WINDOW = Duration.ofMinutes(30);
    private final Map<String, Instant> sentKeys = new ConcurrentHashMap<>();

    @Override
    public boolean isDuplicate(DeduplicationKey key) {
        String keyStr = key.toKeyString();
        Instant sentAt = sentKeys.get(keyStr);
        if (sentAt == null) return false;
        return Instant.now().isBefore(sentAt.plus(DEDUP_WINDOW));
    }

    @Override
    public void markSent(DeduplicationKey key) {
        sentKeys.put(key.toKeyString(), Instant.now());
    }
}
