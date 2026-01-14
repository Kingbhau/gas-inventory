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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex, WebRequest request) {
                logger.warn("Resource not found: {}", ex.getMessage());
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
                logger.warn("Concurrency conflict detected: {}", ex.getMessage());
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
                logger.warn("Optimistic lock failure - concurrent modification detected: {}", ex.getMessage());
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
                logger.warn("Lock timeout - could not acquire database lock: {}", ex.getMessage());
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
                logger.warn("Invalid operation: {}", ex.getMessage());
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
                logger.warn("Illegal argument: {}", ex.getMessage());
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

                logger.warn("Validation error: {}", message);
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
                logger.error("Data integrity violation occurred: {}", message, ex);
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
                logger.error("Database error occurred", ex);
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
                logger.warn("Unsupported media type: {}", ex.getContentType());
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
                logger.warn("HTTP method not supported: {}", ex.getMethod());
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
                logger.error("Unexpected error occurred", ex);

                ErrorResponse error = new ErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "INTERNAL_SERVER_ERROR",
                                "An unexpected error occurred. Please contact support.",
                                LocalDateTime.now());
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}
