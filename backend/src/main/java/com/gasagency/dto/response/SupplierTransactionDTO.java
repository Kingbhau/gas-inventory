package com.gasagency.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class SupplierTransactionDTO {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long supplierId;
    private String supplierName;
    private Long variantId;
    private String variantName;
    private LocalDate transactionDate;
    private Long filledReceived;
    private Long emptyReceived;
    private Long filledSent;
    private Long emptySent;
    private String transactionType;
    private String reference;
    private BigDecimal amount;
    private String note;
    private String createdBy;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String updatedBy;

    public SupplierTransactionDTO() {
    }

    public SupplierTransactionDTO(Long id, Long warehouseId, String warehouseName, Long supplierId, String supplierName,
            Long variantId, String variantName,
            LocalDate transactionDate, Long filledReceived, Long emptyReceived, Long filledSent, Long emptySent,
            String transactionType, String reference, BigDecimal amount, String note) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.variantId = variantId;
        this.variantName = variantName;
        this.transactionDate = transactionDate;
        this.filledReceived = filledReceived;
        this.emptyReceived = emptyReceived;
        this.filledSent = filledSent;
        this.emptySent = emptySent;
        this.transactionType = transactionType;
        this.reference = reference;
        this.amount = amount;
        this.note = note;
    }

    public Double getAmount() {
        return amount != null ? amount.doubleValue() : null;
    }

    public void setAmount(Double amount) {
        this.amount = amount != null ? BigDecimal.valueOf(amount) : null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
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

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}



