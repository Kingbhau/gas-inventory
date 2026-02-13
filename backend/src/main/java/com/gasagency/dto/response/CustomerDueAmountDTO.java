package com.gasagency.dto.response;

import java.math.BigDecimal;

public class CustomerDueAmountDTO {
    private Long customerId;
    private BigDecimal dueAmount;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(BigDecimal dueAmount) {
        this.dueAmount = dueAmount;
    }
}



