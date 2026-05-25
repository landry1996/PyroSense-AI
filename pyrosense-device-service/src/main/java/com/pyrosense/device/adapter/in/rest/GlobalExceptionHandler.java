package com.pyrosense.device.adapter.in.rest;

import com.pyrosense.shared.api.ApiErrorResponse;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.InvalidStateTransitionException;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        var response = ApiErrorResponse.of(
                "NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND.value(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidState(InvalidStateTransitionException ex, HttpServletRequest request) {
        var response = ApiErrorResponse.of(
                "INVALID_STATE_TRANSITION", ex.getMessage(), HttpStatus.CONFLICT.value(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        int status = ex.getErrorCode().httpStatus();
        var response = ApiErrorResponse.of(
                ex.getErrorCode().name(), ex.getMessage(), status, request.getRequestURI());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getViolations().stream()
                .map(v -> new ApiErrorResponse.FieldError(v.field(), v.message(), null))
                .toList();
        var response = ApiErrorResponse.withFieldErrors(
                "VALIDATION_FAILED", ex.getMessage(), HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .toList();
        var response = ApiErrorResponse.withFieldErrors(
                "VALIDATION_FAILED", "Request validation failed", HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }
}
