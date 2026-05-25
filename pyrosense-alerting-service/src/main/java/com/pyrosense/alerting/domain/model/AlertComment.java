package com.pyrosense.alerting.domain.model;

import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AlertComment(
        UUID id,
        UserId author,
        String content,
        Instant createdAt
) {
    public AlertComment {
        Objects.requireNonNull(id);
        Objects.requireNonNull(author);
        Objects.requireNonNull(content);
        if (content.isBlank()) throw new IllegalArgumentException("Comment content must not be blank");
        Objects.requireNonNull(createdAt);
    }

    public static AlertComment create(UserId author, String content) {
        return new AlertComment(UUID.randomUUID(), author, content, ClockProvider.now());
    }
}
