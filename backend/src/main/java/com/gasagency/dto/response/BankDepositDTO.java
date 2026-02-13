package com.gasagency.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BankDepositDTO {
    private Long id;
    private Long bankAccountId;
    private String bankName;
    private String accountNumber;
    private LocalDate depositDate;
    private BigDecimal depositAmount;
    private String referenceNumber;
    private String paymentMode;
    private String notes;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
    private String updatedBy;

    public BankDepositDTO() {
    }

    public BankDepositDTO(Long id, Long bankAccountId, String bankName, String accountNumber,
            LocalDate depositDate, BigDecimal depositAmount,
            String referenceNumber, String paymentMode, String notes) {
        this.id = id;
        this.bankAccountId = bankAccountId;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.depositDate = depositDate;
        this.depositAmount = depositAmount;
        this.referenceNumber = referenceNumber;
        this.paymentMode = paymentMode;
        this.notes = notes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public LocalDate getDepositDate() {
        return depositDate;
    }

    public void setDepositDate(LocalDate depositDate) {
        this.depositDate = depositDate;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "BankDepositDTO{" +
                "id=" + id +
                ", bankAccountId=" + bankAccountId +
                ", bankName='" + bankName + '\'' +
                ", depositDate=" + depositDate +
                ", depositAmount=" + depositAmount +
                ", referenceNumber='" + referenceNumber + '\'' +
                ", paymentMode='" + paymentMode + '\'' +
                '}';
    }
}



