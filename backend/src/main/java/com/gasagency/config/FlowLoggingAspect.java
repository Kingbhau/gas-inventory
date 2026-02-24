package com.gasagency.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Logs controller -> service -> repository -> util flow with timings.
 * Uses INFO for normal flow and ERROR for exceptions (production-safe).
 */
@Aspect
@Component
@ConditionalOnProperty(name = "app.logging.flow.enabled", havingValue = "true", matchIfMissing = true)
public class FlowLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(FlowLoggingAspect.class);
    private static final int MAX_VALUE_LENGTH = 500;
    private static final Set<Class<?>> SKIP_ARG_TYPES = new HashSet<>(Arrays.asList(
            ServletRequest.class,
            ServletResponse.class,
            InputStream.class,
            OutputStream.class
    ));

    @Value("${app.logging.flow.include-args:false}")
    private boolean includeArgs;

    @Around("within(com.gasagency.controller..*) || within(com.gasagency.service..*)")
    public Object logFlow(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String args = includeArgs ? formatArgs(joinPoint.getArgs()) : "[disabled]";

        logger.info("FLOW_ENTER | class={} | method={} | args={}", className, methodName, args);
        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            logger.info("FLOW_EXIT | class={} | method={} | duration={}ms", className, methodName, durationMs);
            return result;
        } catch (Throwable ex) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            logger.error("FLOW_ERROR | class={} | method={} | duration={}ms | error={}",
                    className,
                    methodName,
                    durationMs,
                    ex.getClass().getSimpleName(),
                    ex);
            throw ex;
        }
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Object arg : args) {
            joiner.add(safeToString(arg));
        }
        return joiner.toString();
    }

    private String safeToString(Object arg) {
        if (arg == null) {
            return "null";
        }
        Class<?> argClass = arg.getClass();
        if (SKIP_ARG_TYPES.contains(argClass) || isSubclassOfAny(argClass, SKIP_ARG_TYPES)) {
            return argClass.getSimpleName() + "{skipped}";
        }
        if (arg instanceof byte[]) {
            return "byte[]{skipped}";
        }
        String value = String.valueOf(arg);
        if (value.length() > MAX_VALUE_LENGTH) {
            return value.substring(0, MAX_VALUE_LENGTH) + "...(truncated)";
        }
        return value;
    }

    private boolean isSubclassOfAny(Class<?> clazz, Set<Class<?>> types) {
        for (Class<?> type : types) {
            if (type.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }
}
