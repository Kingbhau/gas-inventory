package com.gasagency.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BankAccountLedgerDTO {
    private Long id;
    private Long bankAccountId;
    private String bankAccountName;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private Long saleId;
    private String referenceNumber;
    private String description;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;

    public BankAccountLedgerDTO() {
    }

    public BankAccountLedgerDTO(Long id, Long bankAccountId, String bankAccountName, String transactionType,
            BigDecimal amount,
            BigDecimal balanceAfter, Long saleId, String referenceNumber,
            String description, LocalDateTime transactionDate, LocalDateTime createdAt) {
        this.id = id;
        this.bankAccountId = bankAccountId;
        this.bankAccountName = bankAccountName;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.saleId = saleId;
        this.referenceNumber = referenceNumber;
        this.description = description;
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
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

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "BankAccountLedgerDTO{" +
                "id=" + id +
                ", bankAccountId=" + bankAccountId +
                ", transactionType='" + transactionType + '\'' +
                ", amount=" + amount +
                ", balanceAfter=" + balanceAfter +
                ", saleId=" + saleId +
                ", referenceNumber='" + referenceNumber + '\'' +
                ", description='" + description + '\'' +
                ", transactionDate=" + transactionDate +
                ", createdAt=" + createdAt +
                '}';
    }
}
