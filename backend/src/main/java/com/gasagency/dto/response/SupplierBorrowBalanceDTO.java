package com.gasagency.dto.response;

public class SupplierBorrowBalanceDTO {
    private Long supplierId;
    private Long warehouseId;
    private Long variantId;
    private Long filledAvailable;
    private Long emptyAvailable;

    public SupplierBorrowBalanceDTO() {
    }

    public SupplierBorrowBalanceDTO(Long supplierId, Long warehouseId, Long variantId,
            Long filledAvailable, Long emptyAvailable) {
        this.supplierId = supplierId;
        this.warehouseId = warehouseId;
        this.variantId = variantId;
        this.filledAvailable = filledAvailable;
        this.emptyAvailable = emptyAvailable;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Long getFilledAvailable() {
        return filledAvailable;
    }

    public void setFilledAvailable(Long filledAvailable) {
        this.filledAvailable = filledAvailable;
    }

    public Long getEmptyAvailable() {
        return emptyAvailable;
    }

    public void setEmptyAvailable(Long emptyAvailable) {
        this.emptyAvailable = emptyAvailable;
    }
}
