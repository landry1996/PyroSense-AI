package com.pyrosense.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExceptionTest {

    @Test
    void businessExceptionShouldCarryErrorCode() {
        var ex = new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Score too high");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
        assertThat(ex.getMessage()).isEqualTo("Score too high");
        assertThat(ex.getErrorCode().httpStatus()).isEqualTo(422);
    }

    @Test
    void notFoundExceptionShouldCarryResourceInfo() {
        var ex = new NotFoundException("Device", "abc-123");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ex.getResourceType()).isEqualTo("Device");
        assertThat(ex.getResourceId()).isEqualTo("abc-123");
        assertThat(ex.getMessage()).contains("Device").contains("abc-123");
    }

    @Test
    void validationExceptionShouldCarryViolations() {
        var violations = List.of(
                new ValidationException.Violation("name", "must not be blank"),
                new ValidationException.Violation("email", "invalid format")
        );
        var ex = new ValidationException("Validation failed", violations);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(ex.getViolations()).hasSize(2);
        assertThat(ex.getViolations().get(0).field()).isEqualTo("name");
        assertThat(ex.getViolations().get(1).message()).isEqualTo("invalid format");
    }

    @Test
    void validationExceptionConvenienceConstructor() {
        var ex = new ValidationException("score", "must be between 0 and 100");
        assertThat(ex.getViolations()).hasSize(1);
        assertThat(ex.getViolations().get(0).field()).isEqualTo("score");
    }

    @Test
    void violationShouldRejectBlankField() {
        assertThatThrownBy(() -> new ValidationException.Violation("", "message"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ValidationException.Violation("  ", "message"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void violationShouldRejectBlankMessage() {
        assertThatThrownBy(() -> new ValidationException.Violation("field", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidStateTransitionShouldFormatMessage() {
        var ex = new InvalidStateTransitionException("Alert", "RESOLVED", "ACKNOWLEDGED");
        assertThat(ex.getMessage()).contains("Alert", "RESOLVED", "ACKNOWLEDGED");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    void errorCodeShouldMapToHttpStatus() {
        assertThat(ErrorCode.NOT_FOUND.httpStatus()).isEqualTo(404);
        assertThat(ErrorCode.VALIDATION_FAILED.httpStatus()).isEqualTo(400);
        assertThat(ErrorCode.INTERNAL_ERROR.httpStatus()).isEqualTo(500);
        assertThat(ErrorCode.UNAUTHORIZED.httpStatus()).isEqualTo(401);
        assertThat(ErrorCode.FORBIDDEN.httpStatus()).isEqualTo(403);
    }
}
