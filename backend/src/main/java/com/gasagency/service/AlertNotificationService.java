package com.gasagency.service;

import com.gasagency.dto.response.AlertNotificationDTO;
import com.gasagency.dto.response.AlertSummaryDTO;
import com.gasagency.entity.AlertNotification;
import com.gasagency.repository.AlertNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing alert notifications
 * Handles creation, dismissal, and cleanup of alerts
 */
@Service
@Transactional
public class AlertNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AlertNotificationService.class);
    private final AlertNotificationRepository repository;
    private final SseService sseService;

    public AlertNotificationService(AlertNotificationRepository repository, SseService sseService) {
        this.repository = repository;
        this.sseService = sseService;
    }

    /**
     * Create or update alert
     * If alert with same key already exists and is not dismissed, return existing
     * Otherwise, create new alert
     */
    public AlertNotification createOrUpdateAlert(String alertType, String alertKey,
            Long warehouseId, Long customerId,
            String message, String severity) {
        // Check if alert already exists and is active
        Optional<AlertNotification> existing = repository.findByAlertKey(alertKey);

        if (existing.isPresent()) {
            AlertNotification alert = existing.get();
            if (!alert.getIsDismissed() && alert.getExpiresAt().isAfter(LocalDateTime.now())) {
                // Alert already active, no need to recreate
                return alert;
            }
        }

        // Create new alert
        AlertNotification alert = new AlertNotification();
        alert.setAlertType(alertType);
        alert.setAlertKey(alertKey);
        alert.setWarehouseId(warehouseId);
        alert.setCustomerId(customerId);
        alert.setMessage(message);
        alert.setSeverity(severity);
        alert.setIsDismissed(false);
        alert.setExpiresAt(LocalDateTime.now().plusHours(24));

        AlertNotification saved = repository.save(alert);
        logger.info("Created alert: {} - {}", alertKey, message);

        // Send via SSE (Real-time)
        sseService.broadcastAlert(saved);

        return saved;
    }

    /**
     * Get all active (non-dismissed, not expired) alerts
     */
    @Transactional(readOnly = true)
    public List<AlertNotification> getActiveAlerts() {
        LocalDateTime now = LocalDateTime.now();
        return repository.findByIsDismissedFalseAndExpiresAtGreaterThan(now);
    }

    @Transactional(readOnly = true)
    public AlertSummaryDTO getActiveAlertSummary() {
        List<AlertNotificationDTO> alerts = getActiveAlerts().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        AlertSummaryDTO summary = new AlertSummaryDTO();
        summary.setCount(alerts.size());
        summary.setAlerts(alerts);
        return summary;
    }

    /**
     * Get all active alerts count
     */
    @Transactional(readOnly = true)
    public int getActiveAlertsCount() {
        return getActiveAlerts().size();
    }

    /**
     * Dismiss alert by id and user
     */
    public void dismissAlert(Long alertId, Long userId) {
        Optional<AlertNotification> alertOpt = repository.findById(alertId);

        if (alertOpt.isPresent()) {
            AlertNotification alert = alertOpt.get();
            alert.setIsDismissed(true);
            alert.setDismissedAt(LocalDateTime.now());
            alert.setDismissedByUserId(userId);

            repository.save(alert);
            logger.info("Alert {} dismissed by user {}", alertId, userId);

            // Broadcast dismissal to all clients
            sseService.broadcastAlertDismissal(alertId);
        }
    }

    /**
     * Delete alert by key (use for cleanup)
     */
    public void deleteAlertByKey(String alertKey) {
        Optional<AlertNotification> alert = repository.findByAlertKey(alertKey);
        alert.ifPresent(repository::delete);
    }

    /**
     * Auto-cleanup expired alerts (runs every hour)
     * Removes alerts that have expired (24 hours passed)
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredAlerts() {
        LocalDateTime now = LocalDateTime.now();
        try {
            repository.deleteByExpiresAtLessThan(now);
            logger.info("Cleanup: Expired alerts removed");
        } catch (Exception e) {
            logger.error("Error cleaning up expired alerts", e);
        }
    }

    private AlertNotificationDTO toDTO(AlertNotification alert) {
        AlertNotificationDTO dto = new AlertNotificationDTO();
        dto.setId(alert.getId());
        dto.setAlertType(alert.getAlertType());
        dto.setAlertKey(alert.getAlertKey());
        dto.setWarehouseId(alert.getWarehouseId());
        dto.setCustomerId(alert.getCustomerId());
        dto.setMessage(alert.getMessage());
        dto.setSeverity(alert.getSeverity());
        dto.setIsDismissed(alert.getIsDismissed());
        dto.setDismissedAt(alert.getDismissedAt());
        dto.setDismissedByUserId(alert.getDismissedByUserId());
        dto.setCreatedAt(alert.getCreatedAt());
        dto.setExpiresAt(alert.getExpiresAt());
        return dto;
    }
}

