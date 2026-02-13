package com.gasagency.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.gasagency.util.ApiError;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import com.gasagency.util.LoggerUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleUsernameNotFound(
                        UsernameNotFoundException ex, WebRequest request) {
                logger.warn("AUTHENTICATION_FAILED | message={}", ex.getMessage());
                LoggerUtil.logBusinessError(logger, "AUTHENTICATION", ex.getMessage());
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", ex.getMessage());
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleResourceNotFound(
                        ResourceNotFoundException ex, WebRequest request) {
                logger.warn("RESOURCE_NOT_FOUND | message={}", ex.getMessage());
                LoggerUtil.logBusinessError(logger, "RESOURCE_LOOKUP", ex.getMessage());
                return buildErrorResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
        }

        @ExceptionHandler(ConcurrencyConflictException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleConcurrencyConflict(
                        ConcurrencyConflictException ex, WebRequest request) {
                logger.warn("CONCURRENCY_CONFLICT | message={}", ex.getMessage());
                LoggerUtil.logConcurrencyIssue(logger, "CONCURRENCY_CONFLICT", "reason", ex.getMessage());
                return buildErrorResponse(HttpStatus.CONFLICT, "CONCURRENCY_CONFLICT", ex.getMessage());
        }

        @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleOptimisticLockingFailure(
                        ObjectOptimisticLockingFailureException ex, WebRequest request) {
                logger.warn("OPTIMISTIC_LOCK_FAILURE | message={}", ex.getMessage());
                LoggerUtil.logConcurrencyIssue(logger, "OPTIMISTIC_LOCKING",
                                "reason", "concurrent_modification", "exception", ex.getClass().getSimpleName());
                return buildErrorResponse(HttpStatus.CONFLICT, "CONCURRENCY_CONFLICT",
                                "The data was modified by another request. Please refresh and try again.");
        }

        @ExceptionHandler(jakarta.persistence.LockTimeoutException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleLockTimeout(
                        jakarta.persistence.LockTimeoutException ex, WebRequest request) {
                logger.warn("LOCK_TIMEOUT | message={}", ex.getMessage());
                LoggerUtil.logConcurrencyIssue(logger, "LOCK_TIMEOUT", "reason", ex.getMessage());
                return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "LOCK_TIMEOUT",
                                "System is busy. Please try again in a moment.");
        }

        @ExceptionHandler(InvalidOperationException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleInvalidOperation(
                        InvalidOperationException ex, WebRequest request) {
                logger.warn("INVALID_OPERATION | message={}", ex.getMessage());
                LoggerUtil.logBusinessError(logger, "INVALID_OPERATION", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_OPERATION", ex.getMessage());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleIllegalArgument(
                        IllegalArgumentException ex, WebRequest request) {
                logger.warn("ILLEGAL_ARGUMENT | message={}", ex.getMessage());
                LoggerUtil.logBusinessError(logger, "ILLEGAL_ARGUMENT", ex.getMessage());
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleValidationException(
                        MethodArgumentNotValidException ex, WebRequest request) {
                String message = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining("; "));

                logger.warn("VALIDATION_ERROR | fields={}", message);
                LoggerUtil.logValidationFailure(logger, "multiple_fields", "N/A", message);
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleConstraintViolation(
                        ConstraintViolationException ex, WebRequest request) {
                String message = ex.getConstraintViolations().stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining("; "));

                logger.warn("CONSTRAINT_VIOLATION | violations={}", message);
                LoggerUtil.logValidationFailure(logger, "constraint", "N/A", message);
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
        }

        @ExceptionHandler(TransactionSystemException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleTransactionSystemException(
                        TransactionSystemException ex, WebRequest request) {
                String message = "An error occurred while processing the transaction.";

                // Check if root cause is ConstraintViolationException
                if (ex.getRootCause() instanceof ConstraintViolationException) {
                        ConstraintViolationException cve = (ConstraintViolationException) ex.getRootCause();
                        message = cve.getConstraintViolations().stream()
                                        .map(ConstraintViolation::getMessage)
                                        .collect(Collectors.joining("; "));
                } else if (ex.getRootCause() != null) {
                        String rootMsg = ex.getRootCause().getMessage();
                        if (rootMsg != null && !rootMsg.isEmpty()) {
                                message = rootMsg;
                        }
                }

                logger.error("TRANSACTION_ERROR | message={} | rootCause={}", message,
                                ex.getRootCause() != null ? ex.getRootCause().getClass().getSimpleName() : "UNKNOWN",
                                ex);
                LoggerUtil.logException(logger, "Transaction system error", ex,
                                "rootCause",
                                ex.getRootCause() != null ? ex.getRootCause().getClass().getSimpleName() : "UNKNOWN");

                return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleDataIntegrityViolation(
                        DataIntegrityViolationException ex, WebRequest request) {
                String message = "Data integrity violation: ";
                String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
                if (cause.contains("username") && cause.contains("already exists")) {
                        message = "Username already exists. Please choose another username.";
                } else if (cause.contains("username") && cause.contains("duplicate key value")) {
                        message = "Username already exists. Please choose another username.";
                } else if (cause.contains("mobile_no") && cause.contains("duplicate key value")) {
                        message = "Mobile number already exists. Please use a different number.";
                } else if (cause.contains("UNIQUE")) {
                        message += "Duplicate entry found.";
                } else if (!cause.isEmpty()) {
                        message += cause;
                } else {
                        message += "A constraint violation occurred.";
                }
                logger.error("DATA_INTEGRITY_VIOLATION | message={} | cause={}", message, cause, ex);
                LoggerUtil.logBusinessError(logger, "DATA_INTEGRITY", message, "cause", cause);
                return buildErrorResponse(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", message);
        }

        @ExceptionHandler(JpaSystemException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleJpaSystemException(
                        JpaSystemException ex, WebRequest request) {
                logger.error("DATABASE_ERROR | message={}", ex.getMessage(), ex);
                LoggerUtil.logException(logger, "Database error", ex, "type", "JpaSystemException");
                return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                                "A database error occurred. Please contact support.");
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleMediaTypeNotSupported(
                        HttpMediaTypeNotSupportedException ex, WebRequest request) {
                logger.warn("UNSUPPORTED_MEDIA_TYPE | contentType={}", ex.getContentType());
                return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                                "Content type not supported. Use application/json");
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<ApiError>> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException ex, WebRequest request) {
                logger.warn("METHOD_NOT_ALLOWED | method={} | supportedMethods={}",
                                ex.getMethod(), ex.getSupportedHttpMethods());
                return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                                "HTTP method " + ex.getMethod() + " is not supported for this endpoint");
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<ApiError>> handleGlobalException(
                        Exception ex, WebRequest request) {
                logger.error("UNEXPECTED_ERROR | exception={} | message={} | cause={}",
                                ex.getClass().getSimpleName(), ex.getMessage(),
                                ex.getCause() != null ? ex.getCause().getMessage() : "null", ex);
                LoggerUtil.logException(logger, "Unexpected error", ex,
                                "exceptionClass", ex.getClass().getSimpleName());

                // Get the actual error message, including root cause if available
                String errorMessage = ex.getMessage();
                if (errorMessage == null || errorMessage.isBlank()) {
                        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                                errorMessage = ex.getCause().getMessage();
                        } else {
                                errorMessage = "An unexpected error occurred. Please contact support.";
                        }
                }

                return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", errorMessage);
        }

        private ResponseEntity<ApiResponse<ApiError>> buildErrorResponse(HttpStatus status, String code,
                        String message) {
                ApiResponse<ApiError> response = ApiResponseUtil.error(message, code);
                return ResponseEntity.status(status).body(response);
        }
}

