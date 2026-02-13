package com.gasagency.dto.response;

import java.time.LocalDateTime;

public class AlertConfigurationDTO {
    private Long id;
    private String alertType;
    private Boolean enabled;
    private Integer filledThreshold;
    private Integer emptyThreshold;
    private Integer pendingReturnThreshold;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public Integer getFilledThreshold() {
        return filledThreshold;
    }

    public void setFilledThreshold(Integer filledThreshold) {
        this.filledThreshold = filledThreshold;
    }

    public Integer getEmptyThreshold() {
        return emptyThreshold;
    }

    public void setEmptyThreshold(Integer emptyThreshold) {
        this.emptyThreshold = emptyThreshold;
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



