package com.gasagency.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * RequestResponseLoggingInterceptor - Logs all API requests and responses
 * Captures timing, status codes, and request/response sizes for debugging and
 * monitoring
 */
@Component
public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "REQUEST_START_TIME";
    private static final String REQUEST_SIZE_ATTR = "REQUEST_SIZE";

    /**
     * Before the actual handler is executed
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTR, startTime);

        // Calculate request size
        int contentLength = request.getContentLength();
        if (contentLength > 0) {
            request.setAttribute(REQUEST_SIZE_ATTR, contentLength);
        }

        // Log incoming request
        logger.info("REQUEST_START | method={} | uri={} | remoteHost={} | contentType={} | contentLength={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteHost(),
                request.getContentType(),
                contentLength > 0 ? contentLength + " bytes" : "N/A");

        return true;
    }

    /**
     * After the handler is executed
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, ModelAndView modelAndView) throws Exception {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;

            logger.info("REQUEST_END | method={} | uri={} | status={} | duration={}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);

            // Log slow requests (> 1 second)
            if (duration > 1000) {
                logger.warn("SLOW_REQUEST | method={} | uri={} | duration={}ms | threshold=1000ms",
                        request.getMethod(),
                        request.getRequestURI(),
                        duration);
            }
        }
    }

    /**
     * After the complete request is finished (called even if an exception occurs)
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) throws Exception {
        if (ex != null) {
            logger.error("REQUEST_ERROR | method={} | uri={} | exception={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getClass().getSimpleName(),
                    ex);
        }
    }
}

