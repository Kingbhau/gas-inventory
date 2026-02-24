package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "supplier_transaction", indexes = {
        @Index(name = "idx_st_transaction_date", columnList = "transaction_date"),
        @Index(name = "idx_st_supplier_date", columnList = "supplier_id, transaction_date"),
        @Index(name = "idx_st_warehouse_supplier", columnList = "warehouse_id, supplier_id"),
        @Index(name = "idx_suppliertransaction_supplier_id", columnList = "supplier_id"),
        @Index(name = "idx_suppliertransaction_variant_id", columnList = "variant_id"),
        @Index(name = "idx_suppliertransaction_transaction_date", columnList = "transaction_date"),
        @Index(name = "idx_suppliertransaction_warehouse_id", columnList = "warehouse_id")
})
public class SupplierTransaction extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Warehouse is required.")
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    @JsonBackReference("warehouse-supplierTransactions")
    private Warehouse warehouse;

    @NotNull(message = "Supplier is required.")
    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @NotNull(message = "Variant is required.")
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonBackReference("variant-supplierTransactions")
    private CylinderVariant variant;

    @NotNull(message = "Transaction date is required.")
    @PastOrPresent(message = "Transaction date cannot be in the future.")
    @Column(nullable = false)
    private LocalDate transactionDate;

    @NotNull(message = "Filled received is required.")
    @Min(value = 0, message = "Filled received cannot be negative.")
    @Column(nullable = false)
    private Long filledReceived;

    @Min(value = 0, message = "Empty received cannot be negative.")
    @Column(nullable = true)
    private Long emptyReceived;

    @Min(value = 0, message = "Filled sent cannot be negative.")
    @Column(nullable = true)
    private Long filledSent;

    @NotNull(message = "Empty sent is required.")
    @Min(value = 0, message = "Empty sent cannot be negative.")
    @Column(nullable = false)
    private Long emptySent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private TransactionType transactionType = TransactionType.PURCHASE;

    @Size(max = 50, message = "Reference must be at most 50 characters.")
    @Column(name = "reference_number", unique = true, nullable = false, length = 50)
    private String reference;

    @Min(value = 0, message = "Amount cannot be negative.")
    @Column(nullable = true, columnDefinition = "DECIMAL(10,2)")
    private BigDecimal amount;

    @Size(max = 500, message = "Note must be at most 500 characters.")
    @Column(nullable = true, length = 500)
    private String note;

    public SupplierTransaction() {
    }

    public SupplierTransaction(Warehouse warehouse, Supplier supplier, CylinderVariant variant,
            LocalDate transactionDate,
            Long filledReceived, Long emptyReceived, Long filledSent, Long emptySent,
            String reference, BigDecimal amount, TransactionType transactionType, String note) {
        this.warehouse = warehouse;
        this.supplier = supplier;
        this.variant = variant;
        this.transactionDate = transactionDate;
        this.filledReceived = filledReceived;
        this.emptyReceived = emptyReceived;
        this.filledSent = filledSent;
        this.emptySent = emptySent;
        this.reference = reference;
        this.amount = amount;
        this.transactionType = transactionType != null ? transactionType : TransactionType.PURCHASE;
        this.note = note;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
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

    public Long getFilledReceived() {
        return filledReceived;
    }

    public void setFilledReceived(Long filledReceived) {
        this.filledReceived = filledReceived;
    }

    public Long getEmptyReceived() {
        return emptyReceived;
    }

    public void setEmptyReceived(Long emptyReceived) {
        this.emptyReceived = emptyReceived;
    }

    public Long getFilledSent() {
        return filledSent;
    }

    public void setFilledSent(Long filledSent) {
        this.filledSent = filledSent;
    }

    public Long getEmptySent() {
        return emptySent;
    }

    public void setEmptySent(Long emptySent) {
        this.emptySent = emptySent;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public enum TransactionType {
        PURCHASE,
        BORROW_IN,
        BORROW_OUT,
        PURCHASE_RETURN
    }
}

