package com.gasagency.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Performance Tracker for monitoring operation execution times
 */
@Component
public class PerformanceTracker {

    private static final Logger performanceLogger = LoggerFactory.getLogger("com.gasagency.performance");
    private static final Logger businessLogger = LoggerFactory.getLogger("com.gasagency.service");
    private static final long SLOW_OPERATION_THRESHOLD_MS = 1000; // 1 second

    /**
     * Track service method execution
     */
    public void trackMethodExecution(String methodName, long durationMs, String description) {
        String logLevel = durationMs > SLOW_OPERATION_THRESHOLD_MS ? "SLOW" : "NORMAL";
        performanceLogger.debug("PERF [{}] {} | method={} | duration={}ms | {}",
                logLevel, MDC.get("requestId"), methodName, durationMs, description);

        if (durationMs > SLOW_OPERATION_THRESHOLD_MS) {
            businessLogger.warn("SLOW_OPERATION | method={} | duration={}ms | threshold={}ms | {}",
                    methodName, durationMs, SLOW_OPERATION_THRESHOLD_MS, description);
        }
    }

    /**
     * Track database query execution
     */
    public void trackDatabaseQuery(String queryType, long durationMs, int rowsAffected) {
        performanceLogger.debug("DB_QUERY | type={} | duration={}ms | rows={}",
                queryType, durationMs, rowsAffected);

        if (durationMs > SLOW_OPERATION_THRESHOLD_MS) {
            businessLogger.warn("SLOW_DATABASE_QUERY | type={} | duration={}ms | rows={}",
                    queryType, durationMs, rowsAffected);
        }
    }

    /**
     * Track external API call
     */
    public void trackExternalCall(String apiName, long durationMs, int httpStatus) {
        performanceLogger.debug("EXTERNAL_API | api={} | duration={}ms | status={}",
                apiName, durationMs, httpStatus);

        if (durationMs > SLOW_OPERATION_THRESHOLD_MS) {
            businessLogger.warn("SLOW_EXTERNAL_API | api={} | duration={}ms | status={}",
                    apiName, durationMs, httpStatus);
        }
    }

    /**
     * Track cache operation
     */
    public void trackCacheOperation(String operation, String cacheKey, long durationMs, boolean cacheHit) {
        performanceLogger.debug("CACHE | operation={} | key={} | duration={}ms | hit={}",
                operation, cacheKey, durationMs, cacheHit);
    }

    /**
     * Track transaction
     */
    public void trackTransaction(String transactionId, long durationMs, String status) {
        performanceLogger.info("TRANSACTION | txn_id={} | duration={}ms | status={}",
                transactionId, durationMs, status);

        if (durationMs > SLOW_OPERATION_THRESHOLD_MS) {
            businessLogger.warn("SLOW_TRANSACTION | txn_id={} | duration={}ms | status={}",
                    transactionId, durationMs, status);
        }
    }

    /**
     * Create a timer for tracking
     */
    public PerformanceTimer startTimer() {
        return new PerformanceTimer();
    }

    /**
     * Inner class for timer functionality
     */
    public static class PerformanceTimer {
        private final long startTime;
        private String timerName;

        public PerformanceTimer() {
            this.startTime = System.currentTimeMillis();
        }

        public PerformanceTimer named(String name) {
            this.timerName = name;
            return this;
        }

        public long stop() {
            return System.currentTimeMillis() - startTime;
        }

        public void stopAndLog(String description) {
            long duration = stop();
            Logger logger = LoggerFactory.getLogger("com.gasagency.performance");
            logger.debug("TIMER [{}] | duration={}ms | {}", timerName, duration, description);
        }
    }
}
