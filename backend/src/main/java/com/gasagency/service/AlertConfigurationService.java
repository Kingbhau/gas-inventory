package com.gasagency.service;

import com.gasagency.entity.AlertConfiguration;
import com.gasagency.repository.AlertConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing alert configurations
 * Handles enable/disable and threshold management for alerts
 * Caches configurations for performance
 */
@Service
public class AlertConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(AlertConfigurationService.class);
    private final AlertConfigurationRepository repository;
    private final ObjectProvider<AlertDetectionService> alertDetectionServiceProvider;

    public AlertConfigurationService(AlertConfigurationRepository repository,
            ObjectProvider<AlertDetectionService> alertDetectionServiceProvider) {
        this.repository = repository;
        this.alertDetectionServiceProvider = alertDetectionServiceProvider;
    }

    @Cacheable(value = "alertConfigCache", key = "#alertType", unless = "#result == null")
    @Transactional(readOnly = true)
    public AlertConfiguration getConfig(String alertType) {
        return repository.findByAlertType(alertType)
                .orElseThrow(() -> new IllegalArgumentException("Alert configuration not found: " + alertType));
    }

    /**
     * Get alert configuration safely (no exception)
     */
    @Transactional(readOnly = true)
    public Optional<AlertConfiguration> getConfigOptional(String alertType) {
        return repository.findByAlertType(alertType);
    }

    /**
     * Get all enabled alert configurations
     */
    @Transactional(readOnly = true)
    public List<AlertConfiguration> getAllEnabledConfigs() {
        return repository.findByEnabled(true);
    }

    /**
     * Get all alert configurations
     */
    @Transactional(readOnly = true)
    public List<AlertConfiguration> getAllConfigs() {
        return repository.findAll();
    }

    /**
     * Update alert configuration (enable/disable and thresholds)
     * Creates configuration if it doesn't exist
     */
    @CacheEvict(value = "alertConfigCache", key = "#alertType")
    @Transactional
    public AlertConfiguration updateAlertConfig(String alertType, Boolean enabled,
            Integer filledThreshold, Integer emptyThreshold,
            Integer pendingReturnThreshold) {
        logger.info("Starting updateAlertConfig for alertType: {}", alertType);

        // Get or create alert configuration
        AlertConfiguration config = repository.findByAlertType(alertType)
                .orElseGet(() -> {
                    logger.info("Creating new AlertConfiguration for type: {}", alertType);
                    AlertConfiguration newConfig = new AlertConfiguration();
                    newConfig.setAlertType(alertType);
                    return newConfig;
                });

        if (enabled != null) {
            config.setEnabled(enabled);
            logger.info("Alert {} {} by admin", alertType, enabled ? "enabled" : "disabled");
        }

        if (filledThreshold != null) {
            config.setFilledCylinderThreshold(filledThreshold);
            logger.info("Alert {} filled threshold updated to {}", alertType, filledThreshold);
        }

        if (emptyThreshold != null) {
            config.setEmptyCylinderThreshold(emptyThreshold);
            logger.info("Alert {} empty threshold updated to {}", alertType, emptyThreshold);
        }

        if (pendingReturnThreshold != null) {
            config.setPendingReturnThreshold(pendingReturnThreshold);
            logger.info("Alert {} pending return threshold updated to {}", alertType, pendingReturnThreshold);
        }

        logger.info("Saving AlertConfiguration for type: {} with enabled: {}", alertType, config.getEnabled());
        AlertConfiguration saved = repository.save(config);
        logger.info("AlertConfiguration saved with id: {}, pendingReturnThreshold: {}", saved.getId(),
                saved.getPendingReturnThreshold());

        // Trigger immediate alert detection after config update (lazy initialization to
        // avoid circular dependency)
        try {
            logger.info("Triggering immediate alert detection after config update");
            AlertDetectionService alertDetectionService = alertDetectionServiceProvider.getIfAvailable();
            if (alertDetectionService != null) {
                alertDetectionService.checkAlertsNow();
            }
        } catch (Exception e) {
            logger.warn("Could not trigger immediate alert detection", e);
        }

        return saved;
    }

    /**
     * Toggle alert enabled status
     */
    @CacheEvict(value = "alertConfigCache", key = "#alertType")
    @Transactional
    public AlertConfiguration toggleAlert(String alertType) {
        AlertConfiguration config = repository.findByAlertType(alertType)
                .orElseGet(() -> {
                    AlertConfiguration newConfig = new AlertConfiguration();
                    newConfig.setAlertType(alertType);
                    return newConfig;
                });
        config.setEnabled(!config.getEnabled());
        logger.info("Alert {} toggled to {}", alertType, config.getEnabled());
        return repository.save(config);
    }
}
