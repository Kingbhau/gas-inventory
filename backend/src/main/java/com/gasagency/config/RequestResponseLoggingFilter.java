package com.gasagency.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Request/Response Logging Filter
 * Logs all HTTP requests and responses with performance metrics
 * Provides visibility into API usage and performance issues
 */
@Component
@ConditionalOnProperty(name = "app.logging.request-response.enabled", havingValue = "true", matchIfMissing = true)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("com.gasagency.performance");

    private static final long SLOW_REQUEST_THRESHOLD = 1000; // milliseconds
    private static final long MEDIUM_REQUEST_THRESHOLD = 500; // milliseconds
    private static final int MAX_BODY_LOG_LENGTH = 4000;
    private static final Set<String> SENSITIVE_HEADERS = new HashSet<>(
            Arrays.asList("authorization", "cookie", "set-cookie"));
    private static final Set<String> SKIP_BODY_CONTENT_TYPES = new HashSet<>(
            Arrays.asList("multipart/form-data", "application/octet-stream"));

    @Value("${app.logging.request-response.include-body:false}")
    private boolean includeBody;

    @Value("${app.logging.request-response.include-headers:false}")
    private boolean includeHeaders;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestId = MDC.get("requestId");

        if (!includeBody) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                int status = response.getStatus();
                String performanceCategory = getPerformanceCategory(duration);
                String query = request.getQueryString();
                String queryPart = query != null && !query.isEmpty() ? query : "N/A";
                String headers = includeHeaders ? getHeaders(request) : "[disabled]";

                logger.info("HTTP_REQUEST | method={} | path={} | query={} | contentType={} | headers={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        queryPart,
                        request.getContentType() != null ? request.getContentType() : "N/A",
                        headers);

                logger.info("HTTP_RESPONSE | method={} | path={} | status={} | duration={}ms | category={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        duration,
                        performanceCategory);

                performanceLogger.debug("{}|method={}|path={}|status={}|duration={}ms|category={}",
                        requestId,
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        duration,
                        performanceCategory);

                if (duration > SLOW_REQUEST_THRESHOLD) {
                    logger.warn("SLOW_REQUEST | method={} | path={} | status={} | duration={}ms",
                            request.getMethod(),
                            request.getRequestURI(),
                            status,
                            duration);
                }
            }
            return;
        }

        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = cachedResponse.getStatus();

            // Determine performance category
            String performanceCategory = getPerformanceCategory(duration);

            String requestBody = getRequestBody(cachedRequest);
            String responseBody = getResponseBody(cachedResponse);
            String headers = includeHeaders ? getHeaders(cachedRequest) : "[disabled]";
            String query = request.getQueryString();
            String queryPart = query != null && !query.isEmpty() ? query : "N/A";

            // Log request with payload
            logger.info("HTTP_REQUEST | method={} | path={} | query={} | contentType={} | headers={} | body={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    queryPart,
                    request.getContentType() != null ? request.getContentType() : "N/A",
                    headers,
                    requestBody);

            // Log response with payload and performance data
            logger.info("HTTP_RESPONSE | method={} | path={} | status={} | duration={}ms | category={} | body={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    duration,
                    performanceCategory,
                    responseBody);

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

            cachedResponse.copyBodyToResponse();
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

    private String getRequestBody(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        if (contentType != null && shouldSkipBody(contentType)) {
            return "[skipped]";
        }
        byte[] buf = request.getContentAsByteArray();
        if (buf == null || buf.length == 0) {
            return "";
        }
        return truncate(toString(buf, request.getCharacterEncoding()));
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        if (contentType != null && shouldSkipBody(contentType)) {
            return "[skipped]";
        }
        byte[] buf = response.getContentAsByteArray();
        if (buf == null || buf.length == 0) {
            return "";
        }
        return truncate(toString(buf, response.getCharacterEncoding()));
    }

    private boolean shouldSkipBody(String contentType) {
        String normalized = contentType.toLowerCase();
        return SKIP_BODY_CONTENT_TYPES.stream().anyMatch(normalized::contains);
    }

    private String toString(byte[] buf, String charsetName) {
        Charset charset;
        try {
            charset = charsetName != null ? Charset.forName(charsetName) : StandardCharsets.UTF_8;
        } catch (Exception e) {
            charset = StandardCharsets.UTF_8;
        }
        return new String(buf, charset);
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= MAX_BODY_LOG_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_BODY_LOG_LENGTH) + "...(truncated)";
    }

    private String getHeaders(HttpServletRequest request) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return "{}";
        }
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String lower = name.toLowerCase();
            String value = SENSITIVE_HEADERS.contains(lower) ? "[masked]" : request.getHeader(name);
            joiner.add(name + "=" + value);
        }
        return joiner.toString();
    }

}

