package com.gasagency.dto.response;

import java.math.BigDecimal;

public class BankDepositSummaryDTO {
    private BigDecimal totalAmount;

    public BankDepositSummaryDTO() {
    }

    public BankDepositSummaryDTO(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}



