package com.gasagency.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.gasagency.util.ConnectionPoolMonitor;

/**
 * AOP Aspect for monitoring transactional methods and tracking concurrent
 * access.
 * Helps identify performance issues and concurrency problems.
 */
@Aspect
@Component
public class TransactionMonitoringAspect {
    private static final Logger logger = LoggerFactory.getLogger(TransactionMonitoringAspect.class);
    private final ConnectionPoolMonitor poolMonitor;

    public TransactionMonitoringAspect(ConnectionPoolMonitor poolMonitor) {
        this.poolMonitor = poolMonitor;
    }

    @Around("execution(* com.gasagency.service.*.*(..)) && @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object monitorTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String transactionId = className + "." + methodName;

        long startTime = System.currentTimeMillis();
        poolMonitor.recordTransactionStart(transactionId);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            if (duration > 5000) {
                logger.warn("Long transaction detected: {}.{} took {}ms", className, methodName, duration);
            }

            poolMonitor.recordTransactionEnd(transactionId);
            return result;
        } catch (Exception e) {
            poolMonitor.recordTransactionError(transactionId, e);
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Transaction failed: {}.{} after {}ms - {}", className, methodName, duration, e.getMessage());
            throw e;
        }
    }
}

