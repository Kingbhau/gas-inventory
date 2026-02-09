package com.gasagency.controller;

import com.gasagency.util.ApiResponse;
import com.gasagency.entity.AlertConfiguration;
import com.gasagency.service.AlertConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Alert Configuration
 * Admin-only endpoints for managing alert settings
 */
@RestController
@RequestMapping("/api/alerts/config")
public class AlertConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(AlertConfigurationController.class);
    private final AlertConfigurationService service;

    public AlertConfigurationController(AlertConfigurationService service) {
        this.service = service;
    }

    /**
     * Get all alert configurations
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertConfiguration>>> getAllConfigs() {
        try {
            List<AlertConfiguration> configs = service.getAllConfigs();
            logger.info("Fetched {} alert configurations", configs.size());
            return ResponseEntity.ok(ApiResponse.success(configs, "Alert configurations retrieved"));
        } catch (Exception e) {
            logger.error("Error fetching alert configurations", e);
            return ResponseEntity.ok(ApiResponse.error(500, "Error fetching alert configurations: " + e.getMessage()));
        }
    }

    /**
     * Get specific alert configuration by type
     */
    @GetMapping("/{alertType}")
    public ResponseEntity<ApiResponse<AlertConfiguration>> getConfig(@PathVariable String alertType) {
        try {
            logger.info("Fetching alert configuration for type: {}", alertType);
            var config = service.getConfigOptional(alertType);
            if (config.isPresent()) {
                logger.info("Found alert configuration: {} with pending threshold: {}",
                        alertType, config.get().getPendingReturnThreshold());
                return ResponseEntity.ok(ApiResponse.success(config.get(), "Alert configuration retrieved"));
            } else {
                logger.info("Alert configuration not found: {}", alertType);
                return ResponseEntity.ok(ApiResponse.error(404, "Alert configuration not found"));
            }
        } catch (Exception e) {
            logger.error("Error fetching alert configuration: {}", alertType, e);
            return ResponseEntity.ok(ApiResponse.error(500, "Error fetching alert configuration: " + e.getMessage()));
        }
    }

    /**
     * Update alert configuration
     * Request body: { "enabled": true/false, "filledThreshold": X,
     * "emptyThreshold": Y, "pendingReturnThreshold": Z }
     */
    @PutMapping("/{alertType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<AlertConfiguration>> updateConfig(
            @PathVariable String alertType,
            @RequestBody Map<String, Object> request) {

        try {
            logger.info("Received update request for alertType: {}", alertType);
            logger.info("Request body: {}", request);

            Boolean enabled = request.get("enabled") != null ? (Boolean) request.get("enabled") : null;
            Integer filledThreshold = request.get("filledThreshold") != null
                    ? ((Number) request.get("filledThreshold")).intValue()
                    : null;
            Integer emptyThreshold = request.get("emptyThreshold") != null
                    ? ((Number) request.get("emptyThreshold")).intValue()
                    : null;
            Integer pendingReturnThreshold = request.get("pendingReturnThreshold") != null
                    ? ((Number) request.get("pendingReturnThreshold")).intValue()
                    : null;

            logger.info("Parsed values - enabled: {}, filled: {}, empty: {}, pending: {}",
                    enabled, filledThreshold, emptyThreshold, pendingReturnThreshold);

            AlertConfiguration updated = service.updateAlertConfig(
                    alertType, enabled, filledThreshold, emptyThreshold, pendingReturnThreshold);

            logger.info("Updated alert configuration: {}", alertType);
            return ResponseEntity.ok(ApiResponse.success(updated, "Alert configuration updated"));
        } catch (Exception e) {
            logger.error("Error updating alert configuration: {}", alertType, e);
            return ResponseEntity.ok(ApiResponse.error(500, "Error updating alert configuration: " + e.getMessage()));
        }
    }

    /**
     * Toggle alert enabled status
     */
    @PostMapping("/{alertType}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<AlertConfiguration>> toggleAlert(@PathVariable String alertType) {
        try {
            AlertConfiguration updated = service.toggleAlert(alertType);
            logger.info("Toggled alert: {} to {}", alertType, updated.getEnabled());
            return ResponseEntity.ok(ApiResponse.success(updated, "Alert toggled"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(404, "Alert configuration not found"));
        }
    }
}
