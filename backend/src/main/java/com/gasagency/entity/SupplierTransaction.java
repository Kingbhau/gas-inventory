package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name = "supplier_transaction", indexes = {
        @Index(name = "idx_suppliertransaction_supplier_id", columnList = "supplier_id"),
        @Index(name = "idx_suppliertransaction_variant_id", columnList = "variant_id"),
        @Index(name = "idx_suppliertransaction_transaction_date", columnList = "transactionDate"),
        @Index(name = "idx_suppliertransaction_warehouse_id", columnList = "warehouse_id")
})
public class SupplierTransaction extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Warehouse is required.")
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @NotNull(message = "Supplier is required.")
    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @NotNull(message = "Variant is required.")
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    private CylinderVariant variant;

    @NotNull(message = "Transaction date is required.")
    @PastOrPresent(message = "Transaction date cannot be in the future.")
    @Column(nullable = false)
    private LocalDate transactionDate;

    @NotNull(message = "Filled received is required.")
    @Min(value = 0, message = "Filled received cannot be negative.")
    @Column(nullable = false)
    private Long filledReceived;

    @NotNull(message = "Empty sent is required.")
    @Min(value = 0, message = "Empty sent cannot be negative.")
    @Column(nullable = false)
    private Long emptySent;

    @Size(max = 255, message = "Reference must be at most 255 characters.")
    @Column(columnDefinition = "TEXT")
    private String reference;

    @NotNull(message = "Amount is required.")
    @Min(value = 0, message = "Amount cannot be negative.")
    @Column(nullable = false)
    private Double amount;

    public SupplierTransaction() {
    }

    public SupplierTransaction(Warehouse warehouse, Supplier supplier, CylinderVariant variant,
            LocalDate transactionDate,
            Long filledReceived, Long emptySent, String reference, Double amount) {
        this.warehouse = warehouse;
        this.supplier = supplier;
        this.variant = variant;
        this.transactionDate = transactionDate;
        this.filledReceived = filledReceived;
        this.emptySent = emptySent;
        this.reference = reference;
        this.amount = amount;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
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

    public Long getEmptySent() {
        return emptySent;
    }

    public void setEmptySent(Long emptySent) {
        this.emptySent = emptySent;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
