package com.gasagency.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monitor and track connection pool usage and concurrent request metrics.
 * Helps identify concurrency bottlenecks and connection pool exhaustion.
 */
@Component
public class ConnectionPoolMonitor {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolMonitor.class);

    private final AtomicInteger activeTransactions = new AtomicInteger(0);
    private final AtomicInteger peakConcurrentRequests = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicInteger> transactionTypeCounter = new ConcurrentHashMap<>();

    public void recordTransactionStart(String transactionType) {
        int current = activeTransactions.incrementAndGet();

        // Track peak concurrent requests
        int peak = peakConcurrentRequests.get();
        while (current > peak && !peakConcurrentRequests.compareAndSet(peak, current)) {
            peak = peakConcurrentRequests.get();
        }

        // Track transaction type counts
        transactionTypeCounter.computeIfAbsent(transactionType, k -> new AtomicInteger(0)).incrementAndGet();

        if (current > 15) {
            logger.warn("High concurrent load detected: {} active transactions", current);
        }
    }

    public void recordTransactionEnd(String transactionType) {
        int current = activeTransactions.decrementAndGet();
        logger.debug("Transaction ended. Active transactions: {}", current);
    }

    public void recordTransactionError(String transactionType, Exception e) {
        activeTransactions.decrementAndGet();
        logger.error("Transaction error in {}: {}", transactionType, e.getMessage());
    }

    public int getActiveTransactionCount() {
        return activeTransactions.get();
    }

    public int getPeakConcurrentRequests() {
        return peakConcurrentRequests.get();
    }

    public int getTransactionTypeCount(String type) {
        AtomicInteger counter = transactionTypeCounter.get(type);
        return counter != null ? counter.get() : 0;
    }

    public void logMetrics() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== CONNECTION POOL METRICS ===\n");
        sb.append("Active Transactions: ").append(activeTransactions.get()).append("\n");
        sb.append("Peak Concurrent Requests: ").append(peakConcurrentRequests.get()).append("\n");
        sb.append("Transaction Type Breakdown:\n");

        transactionTypeCounter
                .forEach((type, count) -> sb.append("  ").append(type).append(": ").append(count.get()).append("\n"));

        logger.info(sb.toString());
    }

    public void reset() {
        activeTransactions.set(0);
        peakConcurrentRequests.set(0);
        transactionTypeCounter.clear();
    }
}

