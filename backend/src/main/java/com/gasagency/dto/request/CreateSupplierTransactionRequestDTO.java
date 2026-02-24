package com.gasagency.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.math.BigDecimal;

public class CreateSupplierTransactionRequestDTO {
    @NotNull(message = "Warehouse ID cannot be null")
    @Positive(message = "Warehouse ID must be greater than 0")
    private Long warehouseId;

    @NotNull(message = "Supplier ID cannot be null")
    @Positive(message = "Supplier ID must be greater than 0")
    private Long supplierId;

    @NotNull(message = "Variant ID cannot be null")
    @Positive(message = "Variant ID must be greater than 0")
    private Long variantId;

    @NotNull(message = "Filled received cannot be null")
    @PositiveOrZero(message = "Filled received must be 0 or positive")
    private Long filledReceived;

    @NotNull(message = "Empty received cannot be null")
    @PositiveOrZero(message = "Empty received must be 0 or positive")
    private Long emptyReceived;

    @NotNull(message = "Filled sent cannot be null")
    @PositiveOrZero(message = "Filled sent must be 0 or positive")
    private Long filledSent;

    @NotNull(message = "Empty sent cannot be null")
    @PositiveOrZero(message = "Empty sent must be 0 or positive")
    private Long emptySent;

    @Size(max = 255, message = "Reference cannot exceed 255 characters")
    private String reference;

    private LocalDate transactionDate;
    @PositiveOrZero(message = "Amount must be 0 or positive")
    private BigDecimal amount;

    private String transactionType;

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;

    public CreateSupplierTransactionRequestDTO() {
    }

    public CreateSupplierTransactionRequestDTO(Long warehouseId, Long supplierId, Long variantId, Long filledReceived,
            Long emptyReceived, Long filledSent, Long emptySent,
            String reference, BigDecimal amount, String transactionType, String note) {
        this.warehouseId = warehouseId;
        this.supplierId = supplierId;
        this.variantId = variantId;
        this.filledReceived = filledReceived;
        this.emptyReceived = emptyReceived;
        this.filledSent = filledSent;
        this.emptySent = emptySent;
        this.reference = reference;
        this.amount = amount;
        this.transactionType = transactionType;
        this.note = note;
    }

    public Double getAmount() {
        return amount != null ? amount.doubleValue() : null;
    }

    public void setAmount(Double amount) {
        this.amount = amount != null ? BigDecimal.valueOf(amount) : null;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
}



