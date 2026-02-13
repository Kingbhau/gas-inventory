package com.gasagency.config;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * GasAgencyRequestContextFilter - Populates MDC (Mapped Diagnostic Context)
 * with request-specific information
 * Ensures all logs include requestId and userId for better traceability and
 * debugging
 * 
 * This filter is called once per request and clears MDC after processing
 */
@Component("gasAgencyRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String USER_ID_MDC_KEY = "userId";
    private static final String USERNAME_MDC_KEY = "username";
    private static final String METHOD_MDC_KEY = "httpMethod";
    private static final String URI_MDC_KEY = "uri";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            // Generate or retrieve request ID
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }
            MDC.put(REQUEST_ID_MDC_KEY, requestId);

            // Extract user information from Spring Security
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                MDC.put(USERNAME_MDC_KEY, username);

                // Try to extract userId if available
                if (authentication.getPrincipal() instanceof UserPrincipal) {
                    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                    MDC.put(USER_ID_MDC_KEY, principal.getUserId().toString());
                }
            }

            // Add request context
            MDC.put(METHOD_MDC_KEY, request.getMethod());
            MDC.put(URI_MDC_KEY, request.getRequestURI());

            // Add request ID to response header for client tracking
            response.addHeader(REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);
        } finally {
            // Always clear MDC to prevent memory leaks
            MDC.clear();
        }
    }

    /**
     * This interface represents a user principal that has a userId
     * Implement this in your UserDetails class
     */
    public interface UserPrincipal {
        Long getUserId();
    }
}

