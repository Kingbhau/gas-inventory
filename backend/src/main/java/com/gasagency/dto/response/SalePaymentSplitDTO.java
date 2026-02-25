package com.gasagency.dto.response;

import java.math.BigDecimal;

public class SalePaymentSplitDTO {
    private Long id;
    private String paymentMode;
    private BigDecimal amount;
    private Long bankAccountId;
    private String bankAccountName;
    private String note;

    public SalePaymentSplitDTO() {
    }

    public SalePaymentSplitDTO(Long id, String paymentMode, BigDecimal amount, Long bankAccountId, String bankAccountName, String note) {
        this.id = id;
        this.paymentMode = paymentMode;
        this.amount = amount;
        this.bankAccountId = bankAccountId;
        this.bankAccountName = bankAccountName;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
