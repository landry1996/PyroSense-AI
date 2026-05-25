package com.pyrosense.shared.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when domain validation fails. Carries a list of field-level violations.
 */
public class ValidationException extends BusinessException {

    private final List<Violation> violations;

    public ValidationException(String message, List<Violation> violations) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.violations = violations != null ? List.copyOf(violations) : List.of();
    }

    public ValidationException(String field, String message) {
        this("Validation failed", List.of(new Violation(field, message)));
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public record Violation(String field, String message) {
        public Violation {
            if (field == null || field.isBlank()) throw new IllegalArgumentException("Field must not be blank");
            if (message == null || message.isBlank()) throw new IllegalArgumentException("Message must not be blank");
        }
    }
}
