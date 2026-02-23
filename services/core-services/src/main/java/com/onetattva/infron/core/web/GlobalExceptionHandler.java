package com.onetattva.infron.core.web;

import com.onetattva.infron.core.services.AuditService;
import com.onetattva.infron.core.web.dto.ErrorResponse;
import com.onetattva.infron.core.web.exceptions.ActionNotAllowedException;
import com.onetattva.infron.core.web.exceptions.PermissionDeniedException;
import com.onetattva.infron.core.web.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 * Provides consistent error responses and audit logging for all exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    private final AuditService auditService;
    
    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }
    
    /**
     * Handle permission denied exceptions.
     * Returns HTTP 403 Forbidden.
     */
    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePermissionDenied(
            PermissionDeniedException ex, WebRequest request) {
        
        logError("Permission denied", ex, request);
        auditService.logAction("permission:denied", ex.getResourceId());
        
        ErrorResponse error = new ErrorResponse(
            "PERMISSION_DENIED",
            ex.getMessage(),
            System.currentTimeMillis()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    /**
     * Handle resource not found exceptions.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        
        logError("Resource not found", ex, request);
        auditService.logAction("resource:not_found", ex.getResourceId());
        
        ErrorResponse error = new ErrorResponse(
            "RESOURCE_NOT_FOUND",
            ex.getMessage(),
            System.currentTimeMillis()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Handle action not allowed exceptions.
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(ActionNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleActionNotAllowed(
            ActionNotAllowedException ex, WebRequest request) {
        
        logError("Action not allowed", ex, request);
        auditService.logAction("action:not_allowed", ex.getResourceId());
        
        ErrorResponse error = new ErrorResponse(
            "ACTION_NOT_ALLOWED",
            ex.getMessage(),
            System.currentTimeMillis()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    /**
     * Handle validation errors from @Valid annotations.
     * Returns HTTP 400 Bad Request with field-level error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        logError("Validation error", ex, request);
        
        // Extract field-level validation errors
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        auditService.logAction("validation:failed", null, validationErrors);
        
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            System.currentTimeMillis(),
            validationErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Handle all other uncaught exceptions.
     * Returns HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        logger.error("Unexpected error occurred", ex);
        auditService.logAction("error:internal", null, Map.of(
            "exception", ex.getClass().getSimpleName(),
            "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
        ));
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            System.currentTimeMillis()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    private void logError(String action, Exception ex, WebRequest request) {
        logger.error("{}: {} - Request: {}", action, ex.getMessage(), request.getDescription(false));
    }
}
