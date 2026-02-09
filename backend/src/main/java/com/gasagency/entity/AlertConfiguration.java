package com.gasagency.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_configuration")
public class AlertConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String alertType; // LOW_STOCK_WAREHOUSE, PENDING_RETURN_CYLINDERS, etc

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column
    private Integer filledCylinderThreshold; // For LOW_STOCK alert

    @Column
    private Integer emptyCylinderThreshold; // For LOW_STOCK alert

    @Column
    private Integer pendingReturnThreshold; // For PENDING_RETURN alert

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getFilledCylinderThreshold() {
        return filledCylinderThreshold;
    }

    public void setFilledCylinderThreshold(Integer filledCylinderThreshold) {
        this.filledCylinderThreshold = filledCylinderThreshold;
    }

    public Integer getEmptyCylinderThreshold() {
        return emptyCylinderThreshold;
    }

    public void setEmptyCylinderThreshold(Integer emptyCylinderThreshold) {
        this.emptyCylinderThreshold = emptyCylinderThreshold;
    }

    public Integer getPendingReturnThreshold() {
        return pendingReturnThreshold;
    }

    public void setPendingReturnThreshold(Integer pendingReturnThreshold) {
        this.pendingReturnThreshold = pendingReturnThreshold;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
