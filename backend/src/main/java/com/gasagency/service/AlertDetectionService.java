package com.gasagency.service;

import com.gasagency.alert.AlertDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Central alert detection service using Strategy Pattern
 * Auto-discovers all AlertDetector implementations
 * Runs periodic checks without needing to modify this class for new alerts
 */
@Service
public class AlertDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(AlertDetectionService.class);
    private final List<AlertDetector> detectors;

    public AlertDetectionService(List<AlertDetector> detectors) {
        this.detectors = detectors;
        logger.info("AlertDetectionService initialized with {} detectors", detectors.size());
        detectors.forEach(d -> logger.info("  - {}", d.getAlertType()));
    }

    /**
     * Run all registered alert detectors
     * This method is called automatically every 5 minutes
     * New alert detectors are automatically picked up without code changes
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkAllAlerts() {
        logger.debug("Running alert detection checks with {} detectors", detectors.size());

        for (AlertDetector detector : detectors) {
            try {
                logger.debug("Running detector: {}", detector.getAlertType());
                detector.detectAndCreateAlerts();
            } catch (Exception e) {
                logger.error("Error in detector {}: {}", detector.getAlertType(), e.getMessage(), e);
            }
        }
    }

    /**
     * Manually trigger alert detection (for testing or immediate checks)
     */
    public void checkAlertsNow() {
        logger.info("Manual alert detection triggered");
        checkAllAlerts();
    }
}

