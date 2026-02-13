package com.gasagency.config;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * MDC (Mapped Diagnostic Context) Filter
 * Sets contextual information for all logs: requestId, userId, transactionId
 * Ensures consistent request tracing across the entire call stack
 */
@Component
public class MDCFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String TRANSACTION_ID_HEADER = "X-Transaction-ID";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";
    private static final String TRANSACTION_ID_KEY = "transactionId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Generate or use provided request ID for distributed tracing
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(REQUEST_ID_KEY, requestId);

        // Set user ID if available (from JWT or session)
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.trim().isEmpty()) {
            MDC.put(USER_ID_KEY, userId);
        }

        // Set transaction ID if provided
        String transactionId = request.getHeader(TRANSACTION_ID_HEADER);
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            MDC.put(TRANSACTION_ID_KEY, transactionId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent memory leaks in thread pools
            MDC.clear();
        }
    }
}

