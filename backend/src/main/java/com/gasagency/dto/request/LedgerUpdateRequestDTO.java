package com.gasagency.dto.request;

import java.math.BigDecimal;

public class LedgerUpdateRequestDTO {
    private Long filledOut;
    private Long emptyIn;
    private BigDecimal totalAmount;
    private BigDecimal amountReceived;
    private String updateReason;
    private String paymentMode;
    private Long bankAccountId;

    public Long getFilledOut() {
        return filledOut;
    }

    public void setFilledOut(Long filledOut) {
        this.filledOut = filledOut;
    }

    public Long getEmptyIn() {
        return emptyIn;
    }

    public void setEmptyIn(Long emptyIn) {
        this.emptyIn = emptyIn;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }

    public String getUpdateReason() {
        return updateReason;
    }

    public void setUpdateReason(String updateReason) {
        this.updateReason = updateReason;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }
}



