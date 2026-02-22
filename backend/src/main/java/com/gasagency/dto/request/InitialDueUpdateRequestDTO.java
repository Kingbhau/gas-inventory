package com.gasagency.dto.request;

import java.math.BigDecimal;

public class InitialDueUpdateRequestDTO {
    private BigDecimal dueAmount;
    private String note;

    public BigDecimal getDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(BigDecimal dueAmount) {
        this.dueAmount = dueAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
