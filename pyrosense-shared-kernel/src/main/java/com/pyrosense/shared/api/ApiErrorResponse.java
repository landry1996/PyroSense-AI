package com.pyrosense.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard API error response (RFC 7807 inspired).
 * Used across all microservices for consistent error format.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp,
        String path,
        List<FieldError> errors
) {

    public static ApiErrorResponse of(String code, String message, int status, String path) {
        return new ApiErrorResponse(code, message, status, Instant.now(), path, null);
    }

    public static ApiErrorResponse withFieldErrors(String code, String message, int status,
                                                    String path, List<FieldError> errors) {
        return new ApiErrorResponse(code, message, status, Instant.now(), path, errors);
    }

    public record FieldError(String field, String message, Object rejectedValue) {}
}
