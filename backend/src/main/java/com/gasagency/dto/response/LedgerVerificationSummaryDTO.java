package com.gasagency.dto.response;

import java.math.BigDecimal;

public class LedgerVerificationSummaryDTO {
    private BigDecimal totalAmount;
    private BigDecimal pendingAmount;
    private BigDecimal verifiedAmount;
    private BigDecimal rejectedAmount;
    private long pendingCount;
    private long verifiedCount;
    private long rejectedCount;

    public LedgerVerificationSummaryDTO() {
        this.totalAmount = BigDecimal.ZERO;
        this.pendingAmount = BigDecimal.ZERO;
        this.verifiedAmount = BigDecimal.ZERO;
        this.rejectedAmount = BigDecimal.ZERO;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(BigDecimal pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    public BigDecimal getVerifiedAmount() {
        return verifiedAmount;
    }

    public void setVerifiedAmount(BigDecimal verifiedAmount) {
        this.verifiedAmount = verifiedAmount;
    }

    public BigDecimal getRejectedAmount() {
        return rejectedAmount;
    }

    public void setRejectedAmount(BigDecimal rejectedAmount) {
        this.rejectedAmount = rejectedAmount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public long getVerifiedCount() {
        return verifiedCount;
    }

    public void setVerifiedCount(long verifiedCount) {
        this.verifiedCount = verifiedCount;
    }

    public long getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(long rejectedCount) {
        this.rejectedCount = rejectedCount;
    }
}
