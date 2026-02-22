package com.gasagency.dto.response;

public class ReturnPendingSummaryDTO {
    private Long totalReturnPending;
    private Long customersWithReturnPending;
    private Long highRiskCount;
    private Integer pendingReturnThreshold;

    public ReturnPendingSummaryDTO() {
    }

    public ReturnPendingSummaryDTO(Long totalReturnPending,
            Long customersWithReturnPending,
            Long highRiskCount,
            Integer pendingReturnThreshold) {
        this.totalReturnPending = totalReturnPending;
        this.customersWithReturnPending = customersWithReturnPending;
        this.highRiskCount = highRiskCount;
        this.pendingReturnThreshold = pendingReturnThreshold;
    }

    public Long getTotalReturnPending() {
        return totalReturnPending;
    }

    public void setTotalReturnPending(Long totalReturnPending) {
        this.totalReturnPending = totalReturnPending;
    }

    public Long getCustomersWithReturnPending() {
        return customersWithReturnPending;
    }

    public void setCustomersWithReturnPending(Long customersWithReturnPending) {
        this.customersWithReturnPending = customersWithReturnPending;
    }

    public Long getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(Long highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public Integer getPendingReturnThreshold() {
        return pendingReturnThreshold;
    }

    public void setPendingReturnThreshold(Integer pendingReturnThreshold) {
        this.pendingReturnThreshold = pendingReturnThreshold;
    }
}
