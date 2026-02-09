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
import com.gasagency.util.LoggerUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleUsernameNotFound(
                        UsernameNotFoundException ex, WebRequest request) {
                logger.warn("AUTHENTICATION_FAILED | message={}", ex.getMessage());
                LoggerUtil.logBusinessError(logger, "AUTHENTICATION", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                "AUTHENTICATION_FAILED",
                                ex.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex, WebRequest request) {
                logger.warn("RESOURCE_NOT_FOUND | message={}", ex.getMessage());
                LoggerUtil.logBusinessError(logger, "RESOURCE_LOOKUP", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                "RESOURCE_NOT_FOUND",
                                ex.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(ConcurrencyConflictException.class)
        public ResponseEntity<ErrorResponse> handleConcurrencyConflict(
                        ConcurrencyConflictException ex, WebRequest request) {
                logger.warn("CONCURRENCY_CONFLICT | message={}", ex.getMessage());
                LoggerUtil.logConcurrencyIssue(logger, "CONCURRENCY_CONFLICT", "reason", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "CONCURRENCY_CONFLICT",
                                ex.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
                        ObjectOptimisticLockingFailureException ex, WebRequest request) {
                logger.warn("OPTIMISTIC_LOCK_FAILURE | message={}", ex.getMessage());
                LoggerUtil.logConcurrencyIssue(logger, "OPTIMISTIC_LOCKING",
                                "reason", "concurrent_modification", "exception", ex.getClass().getSimpleName());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "CONCURRENCY_CONFLICT",
                                "The data was modified by another request. Please refresh and try again.",
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(jakarta.persistence.LockTimeoutException.class)
        public ResponseEntity<ErrorResponse> handleLockTimeout(
                        jakarta.persistence.LockTimeoutException ex, WebRequest request) {
                logger.warn("LOCK_TIMEOUT | message={}", ex.getMessage());
                LoggerUtil.logConcurrencyIssue(logger, "LOCK_TIMEOUT", "reason", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                "LOCK_TIMEOUT",
                                "System is busy. Please try again in a moment.",
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
        }

        @ExceptionHandler(InvalidOperationException.class)
        public ResponseEntity<ErrorResponse> handleInvalidOperation(
                        InvalidOperationException ex, WebRequest request) {
                logger.warn("INVALID_OPERATION | message={}", ex.getMessage());
                LoggerUtil.logBusinessError(logger, "INVALID_OPERATION", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "INVALID_OPERATION",
                                ex.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex, WebRequest request) {
                logger.warn("ILLEGAL_ARGUMENT | message={}", ex.getMessage());
                LoggerUtil.logBusinessError(logger, "ILLEGAL_ARGUMENT", ex.getMessage());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "INVALID_ARGUMENT",
                                ex.getMessage(),
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                        MethodArgumentNotValidException ex, WebRequest request) {
                String message = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining("; "));

                logger.warn("VALIDATION_ERROR | fields={}", message);
                LoggerUtil.logValidationFailure(logger, "multiple_fields", "N/A", message);
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "VALIDATION_ERROR",
                                message,
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex, WebRequest request) {
                String message = ex.getConstraintViolations().stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining("; "));

                logger.warn("CONSTRAINT_VIOLATION | violations={}", message);
                LoggerUtil.logValidationFailure(logger, "constraint", "N/A", message);
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "VALIDATION_ERROR",
                                message,
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(TransactionSystemException.class)
        public ResponseEntity<ErrorResponse> handleTransactionSystemException(
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

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "VALIDATION_ERROR",
                                message,
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
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
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "DATA_INTEGRITY_VIOLATION",
                                message,
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(JpaSystemException.class)
        public ResponseEntity<ErrorResponse> handleJpaSystemException(
                        JpaSystemException ex, WebRequest request) {
                logger.error("DATABASE_ERROR | message={}", ex.getMessage(), ex);
                LoggerUtil.logException(logger, "Database error", ex, "type", "JpaSystemException");
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "DATABASE_ERROR",
                                "A database error occurred. Please contact support.",
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
                        HttpMediaTypeNotSupportedException ex, WebRequest request) {
                logger.warn("UNSUPPORTED_MEDIA_TYPE | contentType={}", ex.getContentType());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                                "UNSUPPORTED_MEDIA_TYPE",
                                "Content type not supported. Use application/json",
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException ex, WebRequest request) {
                logger.warn("METHOD_NOT_ALLOWED | method={} | supportedMethods={}",
                                ex.getMethod(), ex.getSupportedHttpMethods());
                ErrorResponse error = new ErrorResponse(
                                HttpStatus.METHOD_NOT_ALLOWED.value(),
                                "METHOD_NOT_ALLOWED",
                                "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.METHOD_NOT_ALLOWED);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGlobalException(
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

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "INTERNAL_SERVER_ERROR",
                                errorMessage,
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}
