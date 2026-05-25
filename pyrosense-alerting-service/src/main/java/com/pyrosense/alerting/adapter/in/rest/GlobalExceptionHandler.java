package com.pyrosense.alerting.adapter.in.rest;

import com.pyrosense.shared.api.ApiErrorResponse;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.InvalidStateTransitionException;
import com.pyrosense.shared.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(ex.getErrorCode().code(), ex.getMessage(),
                        HttpStatus.NOT_FOUND.value(), null));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex) {
        int status = ex.getErrorCode().httpStatus();
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(ex.getErrorCode().code(), ex.getMessage(), status, null));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
        int status = ex.getErrorCode().httpStatus();
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(ex.getErrorCode().code(), ex.getMessage(), status, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST.value(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST.value(), null));
    }
}
