package com.gasagency.controller;

import com.gasagency.util.ApiResponse;
import com.gasagency.entity.AlertNotification;
import com.gasagency.service.AlertNotificationService;
import com.gasagency.service.SseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Alert Notifications
 * Handles alert retrieval and SSE streaming
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(AlertNotificationController.class);
    private final AlertNotificationService notificationService;
    private final SseService sseService;

    public AlertNotificationController(AlertNotificationService notificationService, SseService sseService) {
        this.notificationService = notificationService;
        this.sseService = sseService;
    }

    /**
     * SSE Stream endpoint for real-time alerts
     * Client establishes persistent connection to receive real-time notifications
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        String userId = authentication != null && authentication.getPrincipal() instanceof UserDetails
                ? ((UserDetails) authentication.getPrincipal()).getUsername()
                : "anonymous_" + System.currentTimeMillis();

        logger.info("User {} subscribed to alerts stream", userId);
        return sseService.subscribe(userId);
    }

    /**
     * Get all active alerts (non-dismissed, not expired)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveAlerts() {
        List<AlertNotification> alerts = notificationService.getActiveAlerts();
        int count = alerts.size();

        logger.info("Fetched {} active alerts", count);

        Map<String, Object> response = Map.of(
                "count", count,
                "alerts", alerts);

        return ResponseEntity.ok(ApiResponse.success(response, "Active alerts retrieved"));
    }

    /**
     * Get alert count only
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getAlertCount() {
        int count = notificationService.getActiveAlertsCount();
        return ResponseEntity.ok(ApiResponse.success(count, "Alert count retrieved"));
    }

    /**
     * Dismiss an alert by id
     */
    @PostMapping("/{alertId}/dismiss")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> dismissAlert(
            @PathVariable Long alertId,
            Authentication authentication) {

        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // Extract user ID from authentication - adjust based on your user entity
            Long userId = 1L; // TODO: Get actual user ID from principal

            notificationService.dismissAlert(alertId, userId);
            logger.info("Alert {} dismissed by user {}", alertId, userDetails.getUsername());

            return ResponseEntity.ok(ApiResponse.success("Alert dismissed successfully"));
        } catch (Exception e) {
            logger.error("Error dismissing alert {}: {}", alertId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(500, "Failed to dismiss alert"));
        }
    }

    /**
     * Get SSE connection count (for monitoring)
     */
    @GetMapping("/connections/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<Integer>> getActiveConnections() {
        int count = sseService.getActiveConnections();
        return ResponseEntity.ok(ApiResponse.success(count, "Active SSE connections"));
    }
}
