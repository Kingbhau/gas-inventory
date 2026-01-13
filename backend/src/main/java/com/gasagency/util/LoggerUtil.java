package com.gasagency.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Centralized logging utility for consistent structured logging across the
 * application
 * Provides methods for common logging patterns: business operations, errors,
 * audit, performance
 */
public class LoggerUtil {

    private static final String AUDIT_LOGGER = "com.gasagency.audit";
    private static final String PERFORMANCE_LOGGER = "com.gasagency.performance";

    private LoggerUtil() {
        // Utility class, no instantiation
    }

    /**
     * Get logger for the given class
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Log business operation entry
     * 
     * @param operation Name of the operation
     * @param details   Key-value pairs describing the operation
     */
    public static void logBusinessEntry(Logger logger, String operation, Object... details) {
        StringBuilder message = new StringBuilder("OPERATION_START | operation=").append(operation);
        appendDetails(message, details);
        logger.info(message.toString());
    }

    /**
     * Log successful business operation completion
     * 
     * @param operation Name of the operation
     * @param details   Key-value pairs describing the result
     */
    public static void logBusinessSuccess(Logger logger, String operation, Object... details) {
        StringBuilder message = new StringBuilder("OPERATION_SUCCESS | operation=").append(operation);
        appendDetails(message, details);
        logger.info(message.toString());
    }

    /**
     * Log business operation failure
     * 
     * @param operation    Name of the operation
     * @param errorMessage Error description
     * @param details      Key-value pairs with additional context
     */
    public static void logBusinessError(Logger logger, String operation, String errorMessage, Object... details) {
        StringBuilder message = new StringBuilder("OPERATION_FAILED | operation=").append(operation)
                .append(" | error=").append(errorMessage);
        appendDetails(message, details);
        logger.warn(message.toString());
    }

    /**
     * Log database operation
     * 
     * @param operation Database operation (SELECT, INSERT, UPDATE, DELETE)
     * @param entity    Entity name
     * @param details   Additional context
     */
    public static void logDatabaseOperation(Logger logger, String operation, String entity, Object... details) {
        StringBuilder message = new StringBuilder("DB_OPERATION | operation=").append(operation)
                .append(" | entity=").append(entity);
        appendDetails(message, details);
        logger.debug(message.toString());
    }

    /**
     * Log audit event (compliance tracking)
     * 
     * @param action  The action taken
     * @param entity  Entity affected
     * @param details Key-value pairs with audit information
     */
    public static void logAudit(String action, String entity, Object... details) {
        Logger auditLogger = LoggerFactory.getLogger(AUDIT_LOGGER);
        StringBuilder message = new StringBuilder("AUDIT | action=").append(action)
                .append(" | entity=").append(entity);
        appendDetails(message, details);
        auditLogger.info(message.toString());
    }

    /**
     * Log performance metric
     * 
     * @param metric     Metric name
     * @param durationMs Duration in milliseconds
     * @param status     Status (SUCCESS, FAILURE, etc.)
     */
    public static void logPerformance(String metric, long durationMs, String status, Object... details) {
        Logger perfLogger = LoggerFactory.getLogger(PERFORMANCE_LOGGER);
        StringBuilder message = new StringBuilder("METRIC | name=").append(metric)
                .append(" | duration=").append(durationMs).append("ms")
                .append(" | status=").append(status);
        appendDetails(message, details);
        perfLogger.debug(message.toString());
    }

    /**
     * Log exception with context
     * 
     * @param logger    Logger instance
     * @param message   Custom message
     * @param exception Exception to log
     * @param details   Key-value pairs with context
     */
    public static void logException(Logger logger, String message, Exception exception, Object... details) {
        StringBuilder logMessage = new StringBuilder("EXCEPTION | message=").append(message);
        appendDetails(logMessage, details);
        logger.error(logMessage.toString(), exception);
    }

    /**
     * Helper method to append key-value pairs to message
     * Expects alternating key, value pattern
     */
    private static void appendDetails(StringBuilder message, Object[] details) {
        if (details != null && details.length > 0) {
            for (int i = 0; i < details.length - 1; i += 2) {
                message.append(" | ").append(details[i]).append("=").append(details[i + 1]);
            }
            // Handle odd number of parameters (incomplete pair)
            if (details.length % 2 == 1) {
                message.append(" | ").append(details[details.length - 1]);
            }
        }
    }

    /**
     * Set transaction ID in MDC for correlation
     * 
     * @param transactionId Unique transaction identifier
     */
    public static void setTransactionId(String transactionId) {
        MDC.put("transactionId", transactionId);
    }

    /**
     * Set user ID in MDC for audit trail
     * 
     * @param userId User identifier
     */
    public static void setUserId(String userId) {
        MDC.put("userId", userId);
    }

    /**
     * Set request ID in MDC for distributed tracing
     * 
     * @param requestId Request identifier
     */
    public static void setRequestId(String requestId) {
        MDC.put("requestId", requestId);
    }

    /**
     * Clear all MDC values
     */
    public static void clearMDC() {
        MDC.clear();
    }
}
