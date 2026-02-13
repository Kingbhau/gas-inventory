package com.gasagency.dto.request;

public class AlertConfigurationUpdateRequestDTO {
    private Boolean enabled;
    private Integer filledThreshold;
    private Integer emptyThreshold;
    private Integer pendingReturnThreshold;

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
}



