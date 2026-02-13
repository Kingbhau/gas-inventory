package com.gasagency.controller;

import com.gasagency.dto.response.AlertConfigurationDTO;
import com.gasagency.dto.request.AlertConfigurationUpdateRequestDTO;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import com.gasagency.service.AlertConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<List<AlertConfigurationDTO>>> getAllConfigs() {
        try {
            List<AlertConfigurationDTO> response = service.getAllConfigDTOs();
            logger.info("Fetched {} alert configurations", response.size());
            return ResponseEntity.ok(ApiResponseUtil.success("Alert configurations retrieved", response));
        } catch (Exception e) {
            logger.error("Error fetching alert configurations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseUtil.error("Error fetching alert configurations: " + e.getMessage(),
                            "ALERT_CONFIG_FETCH_FAILED"));
        }
    }

    /**
     * Get specific alert configuration by type
     */
    @GetMapping("/{alertType}")
    public ResponseEntity<ApiResponse<AlertConfigurationDTO>> getConfig(@PathVariable String alertType) {
        try {
            logger.info("Fetching alert configuration for type: {}", alertType);
            var config = service.getConfigDTOOptional(alertType);
            if (config.isPresent()) {
                logger.info("Found alert configuration: {}", alertType);
                return ResponseEntity.ok(ApiResponseUtil.success("Alert configuration retrieved", config.get()));
            } else {
                logger.info("Alert configuration not found: {}", alertType);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseUtil.error("Alert configuration not found", "RESOURCE_NOT_FOUND"));
            }
        } catch (Exception e) {
            logger.error("Error fetching alert configuration: {}", alertType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseUtil.error("Error fetching alert configuration: " + e.getMessage(),
                            "ALERT_CONFIG_FETCH_FAILED"));
        }
    }

    /**
     * Update alert configuration
     * Request body: { "enabled": true/false, "filledThreshold": X,
     * "emptyThreshold": Y, "pendingReturnThreshold": Z }
     */
    @PutMapping("/{alertType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<AlertConfigurationDTO>> updateConfig(
            @PathVariable String alertType,
            @RequestBody AlertConfigurationUpdateRequestDTO request) {

        try {
            logger.info("Received update request for alertType: {}", alertType);
            logger.info("Request body: {}", request);

            Boolean enabled = request != null ? request.getEnabled() : null;
            Integer filledThreshold = request != null ? request.getFilledThreshold() : null;
            Integer emptyThreshold = request != null ? request.getEmptyThreshold() : null;
            Integer pendingReturnThreshold = request != null ? request.getPendingReturnThreshold() : null;

            logger.info("Parsed values - enabled: {}, filled: {}, empty: {}, pending: {}",
                    enabled, filledThreshold, emptyThreshold, pendingReturnThreshold);

            AlertConfigurationDTO updated = service.updateAlertConfigDTO(
                    alertType, enabled, filledThreshold, emptyThreshold, pendingReturnThreshold);

            logger.info("Updated alert configuration: {}", alertType);
            return ResponseEntity.ok(ApiResponseUtil.success("Alert configuration updated", updated));
        } catch (Exception e) {
            logger.error("Error updating alert configuration: {}", alertType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseUtil.error("Error updating alert configuration: " + e.getMessage(),
                            "ALERT_CONFIG_UPDATE_FAILED"));
        }
    }

    /**
     * Toggle alert enabled status
     */
    @PostMapping("/{alertType}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<AlertConfigurationDTO>> toggleAlert(@PathVariable String alertType) {
        try {
            AlertConfigurationDTO updated = service.toggleAlertDTO(alertType);
            logger.info("Toggled alert: {} to {}", alertType, updated.getEnabled());
            return ResponseEntity.ok(ApiResponseUtil.success("Alert toggled", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseUtil.error("Alert configuration not found", "RESOURCE_NOT_FOUND"));
        }
    }
}

