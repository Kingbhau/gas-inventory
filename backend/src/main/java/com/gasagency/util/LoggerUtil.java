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
     * Log API endpoint entry
     * 
     * @param logger   Logger instance
     * @param method   HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param endpoint API endpoint path
     * @param details  Key-value pairs with request details
     */
    public static void logApiEntry(Logger logger, String method, String endpoint, Object... details) {
        StringBuilder message = new StringBuilder("API_REQUEST | method=").append(method)
                .append(" | endpoint=").append(endpoint);
        appendDetails(message, details);
        logger.info(message.toString());
    }

    /**
     * Log successful API response
     * 
     * @param logger   Logger instance
     * @param method   HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param endpoint API endpoint path
     * @param details  Key-value pairs with response details
     */
    public static void logApiSuccess(Logger logger, String method, String endpoint, Object... details) {
        StringBuilder message = new StringBuilder("API_RESPONSE | method=").append(method)
                .append(" | endpoint=").append(endpoint)
                .append(" | status=SUCCESS");
        appendDetails(message, details);
        logger.info(message.toString());
    }

    /**
     * Log API error response
     * 
     * @param logger       Logger instance
     * @param method       HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param endpoint     API endpoint path
     * @param statusCode   HTTP status code
     * @param errorMessage Error description
     * @param details      Key-value pairs with additional context
     */
    public static void logApiError(Logger logger, String method, String endpoint, int statusCode, String errorMessage,
            Object... details) {
        StringBuilder message = new StringBuilder("API_ERROR | method=").append(method)
                .append(" | endpoint=").append(endpoint)
                .append(" | statusCode=").append(statusCode)
                .append(" | error=").append(errorMessage);
        appendDetails(message, details);
        logger.warn(message.toString());
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

    /**
     * Log validation failure with detailed context
     * 
     * @param logger  Logger instance
     * @param field   Field name that failed validation
     * @param value   Value that failed validation
     * @param reason  Reason for validation failure
     * @param details Additional context key-value pairs
     */
    public static void logValidationFailure(Logger logger, String field, Object value, String reason,
            Object... details) {
        StringBuilder message = new StringBuilder("VALIDATION_FAILED | field=").append(field)
                .append(" | value=").append(value)
                .append(" | reason=").append(reason);
        appendDetails(message, details);
        logger.warn(message.toString());
    }

    /**
     * Log data access (useful for security auditing)
     * 
     * @param logger  Logger instance
     * @param entity  Entity being accessed
     * @param action  Action performed (READ, WRITE, DELETE)
     * @param details Key-value pairs with context
     */
    public static void logDataAccess(Logger logger, String entity, String action, Object... details) {
        StringBuilder message = new StringBuilder("DATA_ACCESS | entity=").append(entity)
                .append(" | action=").append(action);
        appendDetails(message, details);
        logger.info(message.toString());
    }

    /**
     * Log concurrency issue or lock conflict
     * 
     * @param logger    Logger instance
     * @param operation Operation that encountered concurrency issue
     * @param details   Key-value pairs with context
     */
    public static void logConcurrencyIssue(Logger logger, String operation, Object... details) {
        StringBuilder message = new StringBuilder("CONCURRENCY_ISSUE | operation=").append(operation);
        appendDetails(message, details);
        logger.warn(message.toString());
    }

    /**
     * Log calculation details for debugging financial operations
     * 
     * @param logger      Logger instance
     * @param calculation Calculation name
     * @param inputs      Input parameters
     * @param output      Result output
     * @param details     Additional context
     */
    public static void logCalculation(Logger logger, String calculation, String inputs, String output,
            Object... details) {
        StringBuilder message = new StringBuilder("CALCULATION | name=").append(calculation)
                .append(" | inputs=").append(inputs)
                .append(" | output=").append(output);
        appendDetails(message, details);
        logger.debug(message.toString());
    }

    /**
     * Log state transition for business entities
     * 
     * @param logger   Logger instance
     * @param entity   Entity type
     * @param entityId Entity identifier
     * @param oldState Previous state
     * @param newState New state
     * @param details  Additional context
     */
    public static void logStateTransition(Logger logger, String entity, Long entityId, String oldState,
            String newState, Object... details) {
        StringBuilder message = new StringBuilder("STATE_TRANSITION | entity=").append(entity)
                .append(" | id=").append(entityId)
                .append(" | from=").append(oldState)
                .append(" | to=").append(newState);
        appendDetails(message, details);
        logger.info(message.toString());
    }
}

