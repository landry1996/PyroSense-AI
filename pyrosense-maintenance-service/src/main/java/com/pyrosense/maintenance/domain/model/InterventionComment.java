package com.pyrosense.maintenance.domain.model;

import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InterventionComment(
        UUID id,
        UserId authorId,
        String content,
        Instant createdAt
) {
    public InterventionComment {
        Objects.requireNonNull(id);
        Objects.requireNonNull(authorId);
        Objects.requireNonNull(content);
        if (content.isBlank()) throw new IllegalArgumentException("Comment content must not be blank");
        if (createdAt == null) createdAt = ClockProvider.now();
    }

    public static InterventionComment create(UserId authorId, String content) {
        return new InterventionComment(UUID.randomUUID(), authorId, content, ClockProvider.now());
    }
}
