package com.gasagency.dto;

import java.time.LocalDate;

public class SupplierTransactionDTO {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long variantId;
    private String variantName;
    private LocalDate transactionDate;
    private Long filledReceived;
    private Long emptySent;
    private String reference;
    private Double amount;

    public SupplierTransactionDTO() {
    }

    public SupplierTransactionDTO(Long id, Long supplierId, String supplierName, Long variantId, String variantName,
            LocalDate transactionDate, Long filledReceived, Long emptySent, String reference, Double amount) {
        this.id = id;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.variantId = variantId;
        this.variantName = variantName;
        this.transactionDate = transactionDate;
        this.filledReceived = filledReceived;
        this.emptySent = emptySent;
        this.reference = reference;
        this.amount = amount;
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

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
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
