package com.gasagency.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Request/Response Logging Filter
 * Logs all HTTP requests and responses with performance metrics
 * Provides visibility into API usage and performance issues
 */
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("com.gasagency.performance");

    private static final long SLOW_REQUEST_THRESHOLD = 1000; // milliseconds
    private static final long MEDIUM_REQUEST_THRESHOLD = 500; // milliseconds

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestId = MDC.get("requestId");

        // Log incoming request
        logger.info("HTTP_REQUEST | method={} | path={} | contentType={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getContentType() != null ? request.getContentType() : "N/A");

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // Determine performance category
            String performanceCategory = getPerformanceCategory(duration);

            // Log response with performance data
            logger.info("HTTP_RESPONSE | method={} | path={} | status={} | duration={}ms | category={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    duration,
                    performanceCategory);

            // Log performance metrics in structured format
            performanceLogger.debug("{}|method={}|path={}|status={}|duration={}ms|category={}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    duration,
                    performanceCategory);

            // Alert on slow requests
            if (duration > SLOW_REQUEST_THRESHOLD) {
                logger.warn("SLOW_REQUEST | method={} | path={} | status={} | duration={}ms",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        duration);
            }
        }
    }

    /**
     * Categorize request performance
     */
    private String getPerformanceCategory(long durationMs) {
        if (durationMs > SLOW_REQUEST_THRESHOLD) {
            return "SLOW";
        } else if (durationMs > MEDIUM_REQUEST_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "FAST";
        }
    }

}
