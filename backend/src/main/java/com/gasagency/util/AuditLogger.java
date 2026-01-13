package com.gasagency.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Audit Logger for compliance and accountability
 * Logs all important business operations
 */
@Component
public class AuditLogger {

    private static final Logger auditLogger = LoggerFactory.getLogger("com.gasagency.audit");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log sale creation
     */
    public void logSaleCreated(Long saleId, Long customerId, String customerName, Double totalAmount) {
        auditLogger.info("SALE_CREATED | saleId={} | customerId={} | customer={} | amount={} | timestamp={}",
                saleId, customerId, customerName, totalAmount, getCurrentTimestamp());
    }

    /**
     * Log sale modification
     */
    public void logSaleModified(Long saleId, String reason, String modifiedBy) {
        auditLogger.info("SALE_MODIFIED | saleId={} | reason={} | modifiedBy={} | timestamp={}",
                saleId, reason, modifiedBy, getCurrentTimestamp());
    }

    /**
     * Log customer creation
     */
    public void logCustomerCreated(Long customerId, String customerName, String mobile) {
        auditLogger.info("CUSTOMER_CREATED | customerId={} | name={} | mobile={} | timestamp={}",
                customerId, customerName, mobile, getCurrentTimestamp());
    }

    /**
     * Log customer modification
     */
    public void logCustomerModified(Long customerId, String fieldModified, String oldValue, String newValue) {
        auditLogger.info("CUSTOMER_MODIFIED | customerId={} | field={} | oldValue={} | newValue={} | timestamp={}",
                customerId, fieldModified, oldValue, newValue, getCurrentTimestamp());
    }

    /**
     * Log inventory update
     */
    public void logInventoryUpdated(Long variantId, String variantName, String operation, Long quantity) {
        auditLogger.info("INVENTORY_UPDATED | variantId={} | variant={} | operation={} | quantity={} | timestamp={}",
                variantId, variantName, operation, quantity, getCurrentTimestamp());
    }

    /**
     * Log price change
     */
    public void logPriceChanged(Long variantId, String variantName, Double oldPrice, Double newPrice) {
        auditLogger.info("PRICE_CHANGED | variantId={} | variant={} | oldPrice={} | newPrice={} | timestamp={}",
                variantId, variantName, oldPrice, newPrice, getCurrentTimestamp());
    }

    /**
     * Log access attempt (successful)
     */
    public void logAccessGranted(String userId, String resource, String action) {
        auditLogger.info("ACCESS_GRANTED | userId={} | resource={} | action={} | timestamp={}",
                userId, resource, action, getCurrentTimestamp());
    }

    /**
     * Log access attempt (failed)
     */
    public void logAccessDenied(String userId, String resource, String reason) {
        auditLogger.warn("ACCESS_DENIED | userId={} | resource={} | reason={} | timestamp={}",
                userId, resource, reason, getCurrentTimestamp());
    }

    /**
     * Log data export
     */
    public void logDataExported(String exportType, Long recordCount, String exportedBy) {
        auditLogger.info("DATA_EXPORTED | type={} | records={} | exportedBy={} | timestamp={}",
                exportType, recordCount, exportedBy, getCurrentTimestamp());
    }

    /**
     * Log data deletion
     */
    public void logDataDeleted(String entityType, Long entityId, String deletedBy, String reason) {
        auditLogger.info("DATA_DELETED | entityType={} | entityId={} | deletedBy={} | reason={} | timestamp={}",
                entityType, entityId, deletedBy, reason, getCurrentTimestamp());
    }

    /**
     * Log configuration change
     */
    public void logConfigurationChanged(String configKey, String oldValue, String newValue, String changedBy) {
        auditLogger.info("CONFIG_CHANGED | key={} | oldValue={} | newValue={} | changedBy={} | timestamp={}",
                configKey, oldValue, newValue, changedBy, getCurrentTimestamp());
    }

    /**
     * Log system event
     */
    public void logSystemEvent(String eventType, String description) {
        auditLogger.info("SYSTEM_EVENT | type={} | description={} | timestamp={}",
                eventType, description, getCurrentTimestamp());
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(formatter);
    }

    /**
     * Get current request ID from MDC
     */
    public static String getRequestId() {
        return MDC.get("requestId");
    }

    /**
     * Get current user ID from MDC
     */
    public static String getUserId() {
        return MDC.get("userId");
    }
}
