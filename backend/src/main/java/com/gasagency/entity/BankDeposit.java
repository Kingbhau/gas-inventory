package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bank_deposit", indexes = {
        @Index(name = "idx_bank_deposit_date", columnList = "deposit_date"),
        @Index(name = "idx_bank_deposit_account_date", columnList = "bank_account_id, deposit_date"),
        @Index(name = "idx_bank_deposit_reference", columnList = "reference_number")
})
public class BankDeposit extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotNull(message = "Bank account is required.")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @NotNull(message = "Deposit date is required.")
    @PastOrPresent(message = "Deposit date cannot be in the future.")
    @Column(nullable = false)
    private LocalDate depositDate;

    @NotNull(message = "Deposit amount is required.")
    @DecimalMin(value = "0.01", message = "Deposit amount must be greater than 0.")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal depositAmount;

    @Column(nullable = true, length = 50)
    private String referenceNumber;

    @NotBlank(message = "Payment mode is required.")
    @Column(nullable = false, length = 50)
    private String paymentMode;

    @Column(length = 500)
    private String notes;

    public BankDeposit() {
    }

    public BankDeposit(BankAccount bankAccount, LocalDate depositDate, BigDecimal depositAmount,
            String referenceNumber, String paymentMode) {
        this.bankAccount = bankAccount;
        this.depositDate = depositDate;
        this.depositAmount = depositAmount;
        this.referenceNumber = referenceNumber;
        this.paymentMode = paymentMode;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
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

    @Override
    public String toString() {
        return "BankDeposit{" +
                "id=" + id +
                ", depositDate=" + depositDate +
                ", depositAmount=" + depositAmount +
                ", referenceNumber='" + referenceNumber + '\'' +
                ", paymentMode='" + paymentMode + '\'' +
                '}';
    }
}
