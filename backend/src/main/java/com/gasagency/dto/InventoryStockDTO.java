package com.gasagency.dto;

import java.time.LocalDateTime;

public class InventoryStockDTO {
    private Long id;
    private Long variantId;
    private String variantName;
    private Long filledQty;
    private Long emptyQty;
    private LocalDateTime lastUpdated;

    public InventoryStockDTO() {
    }

    public InventoryStockDTO(Long id, Long variantId, String variantName, Long filledQty, Long emptyQty,
            LocalDateTime lastUpdated) {
        this.id = id;
        this.variantId = variantId;
        this.variantName = variantName;
        this.filledQty = filledQty;
        this.emptyQty = emptyQty;
        this.lastUpdated = lastUpdated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
