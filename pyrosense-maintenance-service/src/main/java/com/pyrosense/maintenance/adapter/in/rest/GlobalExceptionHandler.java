package com.pyrosense.maintenance.adapter.in.rest;

import com.pyrosense.shared.api.ApiErrorResponse;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.InvalidStateTransitionException;
import com.pyrosense.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, WebRequest request) {
        var response = ApiErrorResponse.of("NOT_FOUND", ex.getMessage(),
                HttpStatus.NOT_FOUND.value(), extractPath(request));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, WebRequest request) {
        var errorCode = ex.getErrorCode();
        var response = ApiErrorResponse.of(errorCode.code(), ex.getMessage(),
                errorCode.httpStatus(), extractPath(request));
        return ResponseEntity.status(errorCode.httpStatus()).body(response);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex,
                                                                     WebRequest request) {
        var response = ApiErrorResponse.of("INVALID_STATE_TRANSITION", ex.getMessage(),
                HttpStatus.CONFLICT.value(), extractPath(request));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        var response = ApiErrorResponse.of("BAD_REQUEST", ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(), extractPath(request));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<ApiErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .toList();
        var response = ApiErrorResponse.withFieldErrors("VALIDATION_FAILED", "Validation failed",
                HttpStatus.BAD_REQUEST.value(), extractPath(request), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        var response = ApiErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), extractPath(request));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
