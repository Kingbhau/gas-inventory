package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

@Entity
@Table(name = "customer_cylinder_ledger", indexes = {
        @Index(name = "idx_ledger_customer_id", columnList = "customer_id"),
        @Index(name = "idx_ledger_variant_id", columnList = "variant_id"),
        @Index(name = "idx_ledger_transaction_date", columnList = "transactionDate")
})
public class CustomerCylinderLedger extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Customer is required.")
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Variant is required.")
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    private CylinderVariant variant;

    @NotNull(message = "Transaction date is required.")
    @PastOrPresent(message = "Transaction date cannot be in the future.")
    @Column(nullable = false)
    private LocalDate transactionDate;

    @NotNull(message = "Reference type is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType refType;

    // Reference ID is only required for transactions that reference another entity
    // (e.g., SALE, PURCHASE)
    @Column(nullable = true)
    private Long refId;

    @NotNull(message = "Filled out is required.")
    @Min(value = 0, message = "Filled out cannot be negative.")
    @Column(nullable = false)
    private Long filledOut;

    @NotNull(message = "Empty in is required.")
    @Min(value = 0, message = "Empty in cannot be negative.")
    @Column(nullable = false)
    private Long emptyIn;

    @NotNull(message = "Balance is required.")
    @Min(value = 0, message = "Balance cannot be negative.")
    @Column(nullable = false)
    private Long balance;

    public CustomerCylinderLedger() {
    }

    public CustomerCylinderLedger(Customer customer, CylinderVariant variant, LocalDate transactionDate,
            TransactionType refType, Long refId, Long filledOut, Long emptyIn, Long balance) {
        this.customer = customer;
        this.variant = variant;
        this.transactionDate = transactionDate;
        this.refType = refType;
        this.refId = refId;
        this.filledOut = filledOut;
        this.emptyIn = emptyIn;
        this.balance = balance;
    }

    public enum TransactionType {
        SALE, EMPTY_RETURN
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public CylinderVariant getVariant() {
        return variant;
    }

    public void setVariant(CylinderVariant variant) {
        this.variant = variant;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public TransactionType getRefType() {
        return refType;
    }

    public void setRefType(TransactionType refType) {
        this.refType = refType;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public Long getFilledOut() {
        return filledOut;
    }

    public void setFilledOut(Long filledOut) {
        this.filledOut = filledOut;
    }

    public Long getEmptyIn() {
        return emptyIn;
    }

    public void setEmptyIn(Long emptyIn) {
        this.emptyIn = emptyIn;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }
}
