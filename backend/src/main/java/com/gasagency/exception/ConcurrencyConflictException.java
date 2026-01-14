package com.gasagency.exception;

/**
 * Exception thrown when optimistic lock fails due to concurrent modifications
 * Occurs when entity version mismatch detected during update
 */
public class ConcurrencyConflictException extends RuntimeException {
    private final String entityName;
    private final Long entityId;

    public ConcurrencyConflictException(String message) {
        super(message);
        this.entityName = null;
        this.entityId = null;
    }

    public ConcurrencyConflictException(String entityName, Long entityId) {
        super("Concurrent modification detected for " + entityName + " (ID: " + entityId +
                "). Please retry the operation.");
        this.entityName = entityName;
        this.entityId = entityId;
    }

    public ConcurrencyConflictException(String message, String entityName, Long entityId) {
        super(message);
        this.entityName = entityName;
        this.entityId = entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public Long getEntityId() {
        return entityId;
    }
}
