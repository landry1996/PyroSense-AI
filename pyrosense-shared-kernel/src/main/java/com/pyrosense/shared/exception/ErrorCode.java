package com.pyrosense.shared.exception;

/**
 * Enumeration of standard error codes across the platform.
 * Each code maps to an HTTP status and a stable machine-readable identifier.
 */
public enum ErrorCode {

    // 4xx - Client errors
    VALIDATION_FAILED("VALIDATION_FAILED", 400),
    NOT_FOUND("NOT_FOUND", 404),
    CONFLICT("CONFLICT", 409),
    UNAUTHORIZED("UNAUTHORIZED", 401),
    FORBIDDEN("FORBIDDEN", 403),

    // 5xx - Server errors
    INTERNAL_ERROR("INTERNAL_ERROR", 500),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", 503),

    // Business errors
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION", 422),
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", 422),
    THRESHOLD_EXCEEDED("THRESHOLD_EXCEEDED", 422);

    private final String code;
    private final int httpStatus;

    ErrorCode(String code, int httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
