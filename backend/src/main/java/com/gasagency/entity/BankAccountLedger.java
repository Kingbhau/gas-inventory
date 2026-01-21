package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "bank_account_ledger")
public class BankAccountLedger extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bank_account_id", nullable = false)
    @JsonBackReference("bankAccount-accountLedgers")
    private BankAccount bankAccount;

    @Column(nullable = false)
    private String transactionType; // DEPOSIT, WITHDRAWAL, ADJUSTMENT

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = true)
    private BigDecimal balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = true)
    @JsonBackReference("bankAccount-ledgers")
    private Sale sale; // Reference to Sale if transaction is from a sale

    @Pattern(regexp = "^(DEP|WIT|ADJ)-[A-Z0-9]+-\\d{6}-\\d{6}$", message = "Reference must match format: (DEP|WIT|ADJ)-CODE-YYYYMM-SEQUENCE")
    @Column(name = "reference_number", nullable = false, length = 50, unique = true)
    private String referenceNumber;

    @Column(name = "description")
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    public BankAccountLedger() {
        this.transactionDate = LocalDateTime.now();
    }

    public BankAccountLedger(BankAccount bankAccount, String transactionType, BigDecimal amount,
            BigDecimal balanceAfter, Sale sale, String referenceNumber, String description) {
        this();
        this.bankAccount = bankAccount;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.sale = sale;
        this.referenceNumber = referenceNumber;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
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

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
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

    @Override
    public String toString() {
        return "BankAccountLedger{" +
                "id=" + id +
                ", bankAccount=" + bankAccount +
                ", transactionType='" + transactionType + '\'' +
                ", amount=" + amount +
                ", balanceAfter=" + balanceAfter +
                ", sale=" + (sale != null ? sale.getId() : null) +
                ", referenceNumber='" + referenceNumber + '\'' +
                ", description='" + description + '\'' +
                ", transactionDate=" + transactionDate +
                '}';
    }
}
