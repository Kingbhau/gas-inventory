package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_ledger_payment_split", indexes = {
        @Index(name = "idx_clps_ledger", columnList = "ledger_id"),
        @Index(name = "idx_clps_mode", columnList = "payment_mode"),
        @Index(name = "idx_clps_bank", columnList = "bank_account_id")
})
public class CustomerLedgerPaymentSplit extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_id", nullable = false)
    private CustomerCylinderLedger ledger;

    @NotNull
    @Column(name = "payment_mode", nullable = false, length = 50)
    private String paymentMode;

    @NotNull
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Size(max = 255)
    @Column(length = 255)
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerCylinderLedger getLedger() {
        return ledger;
    }

    public void setLedger(CustomerCylinderLedger ledger) {
        this.ledger = ledger;
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

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
