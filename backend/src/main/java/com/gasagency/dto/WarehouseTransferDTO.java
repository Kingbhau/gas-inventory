package com.gasagency.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class WarehouseTransferDTO {
    private Long id;

    @NotNull(message = "From warehouse ID is required.")
    private Long fromWarehouseId;

    @NotNull(message = "To warehouse ID is required.")
    private Long toWarehouseId;

    @NotNull(message = "Variant ID is required.")
    private Long variantId;

    @NotNull(message = "Filled quantity is required.")
    @Min(value = 0, message = "Filled quantity cannot be negative.")
    private Long filledQty;

    @NotNull(message = "Empty quantity is required.")
    @Min(value = 0, message = "Empty quantity cannot be negative.")
    private Long emptyQty;

    private LocalDate transferDate;
    private LocalDateTime createdAt;
    private String notes;

    // Display fields
    private String fromWarehouseName;
    private String toWarehouseName;
    private String variantName;
    private Long version;

    public WarehouseTransferDTO() {
    }

    public WarehouseTransferDTO(Long fromWarehouseId, Long toWarehouseId, Long variantId,
            Long filledQty, Long emptyQty) {
        this.fromWarehouseId = fromWarehouseId;
        this.toWarehouseId = toWarehouseId;
        this.variantId = variantId;
        this.filledQty = filledQty;
        this.emptyQty = emptyQty;
        this.transferDate = LocalDate.now();
    }

    public WarehouseTransferDTO(Long fromWarehouseId, Long toWarehouseId, Long variantId,
            Long filledQty, Long emptyQty, String notes) {
        this.fromWarehouseId = fromWarehouseId;
        this.toWarehouseId = toWarehouseId;
        this.variantId = variantId;
        this.filledQty = filledQty;
        this.emptyQty = emptyQty;
        this.notes = notes;
        this.transferDate = LocalDate.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromWarehouseId() {
        return fromWarehouseId;
    }

    public void setFromWarehouseId(Long fromWarehouseId) {
        this.fromWarehouseId = fromWarehouseId;
    }

    public Long getToWarehouseId() {
        return toWarehouseId;
    }

    public void setToWarehouseId(Long toWarehouseId) {
        this.toWarehouseId = toWarehouseId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Long getFilledQty() {
        return filledQty;
    }

    public void setFilledQty(Long filledQty) {
        this.filledQty = filledQty;
    }

    public Long getEmptyQty() {
        return emptyQty;
    }

    public void setEmptyQty(Long emptyQty) {
        this.emptyQty = emptyQty;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFromWarehouseName() {
        return fromWarehouseName;
    }

    public void setFromWarehouseName(String fromWarehouseName) {
        this.fromWarehouseName = fromWarehouseName;
    }

    public String getToWarehouseName() {
        return toWarehouseName;
    }

    public void setToWarehouseName(String toWarehouseName) {
        this.toWarehouseName = toWarehouseName;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "WarehouseTransferDTO{" +
                "id=" + id +
                ", fromWarehouse=" + fromWarehouseName +
                ", toWarehouse=" + toWarehouseName +
                ", variant=" + variantName +
                ", filledQty=" + filledQty +
                ", emptyQty=" + emptyQty +
                ", transferDate=" + transferDate +
                '}';
    }
}
