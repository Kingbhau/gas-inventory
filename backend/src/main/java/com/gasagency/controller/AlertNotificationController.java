package com.gasagency.controller;

import com.gasagency.dto.response.AlertSummaryDTO;
import com.gasagency.entity.User;
import com.gasagency.repository.UserRepository;
import com.gasagency.service.AlertNotificationService;
import com.gasagency.service.SseService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


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
    private final UserRepository userRepository;

    public AlertNotificationController(AlertNotificationService notificationService, SseService sseService,
            UserRepository userRepository) {
        this.notificationService = notificationService;
        this.sseService = sseService;
        this.userRepository = userRepository;
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
    public ResponseEntity<ApiResponse<AlertSummaryDTO>> getActiveAlerts() {
        AlertSummaryDTO response = notificationService.getActiveAlertSummary();
        logger.info("Fetched {} active alerts", response.getCount());
        return ResponseEntity.ok(ApiResponseUtil.success("Active alerts retrieved", response));
    }

    /**
     * Get alert count only
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getAlertCount() {
        int count = notificationService.getActiveAlertsCount();
        return ResponseEntity.ok(ApiResponseUtil.success("Alert count retrieved", count));
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
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("User not found"));

            notificationService.dismissAlert(alertId, user.getId());
            logger.info("Alert {} dismissed by user {}", alertId, userDetails.getUsername());

            return ResponseEntity.ok(ApiResponseUtil.success("Alert dismissed successfully"));
        } catch (Exception e) {
            logger.error("Error dismissing alert {}: {}", alertId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseUtil.error("Failed to dismiss alert", "ALERT_DISMISS_FAILED"));
        }
    }

    /**
     * Get SSE connection count (for monitoring)
     */
    @GetMapping("/connections/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<Integer>> getActiveConnections() {
        int count = sseService.getActiveConnections();
        return ResponseEntity.ok(ApiResponseUtil.success("Active SSE connections", count));
    }

}

