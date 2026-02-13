package com.gasagency.alert;

/**
 * Strategy pattern interface for extensible alert detection
 * Each alert type implements this interface
 * Allows adding new alerts without modifying existing code
 */
public interface AlertDetector {

    /**
     * Get the alert type this detector handles
     */
    String getAlertType();

    /**
     * Detect and create alerts based on current system state
     * Should be idempotent - safe to call multiple times
     */
    void detectAndCreateAlerts();
}

