package com.gasagency.dto.response;

import java.math.BigDecimal;

public class PaymentsSummaryDTO {
    private BigDecimal totalAmount;

    public PaymentsSummaryDTO() {
    }

    public PaymentsSummaryDTO(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}



