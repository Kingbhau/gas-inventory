package com.gasagency.dto;

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
    private Long emptySent;
    private String reference;
    private BigDecimal amount;
    private String createdBy;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String updatedBy;

    public SupplierTransactionDTO() {
    }

    public SupplierTransactionDTO(Long id, Long warehouseId, String warehouseName, Long supplierId, String supplierName,
            Long variantId, String variantName,
            LocalDate transactionDate, Long filledReceived, Long emptySent, String reference, BigDecimal amount) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
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
