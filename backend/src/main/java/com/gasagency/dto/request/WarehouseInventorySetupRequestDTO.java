package com.gasagency.dto.request;

import java.util.List;

public class WarehouseInventorySetupRequestDTO {
    private Long warehouseId;
    private List<WarehouseInventoryItemDTO> inventoryItems;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public List<WarehouseInventoryItemDTO> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<WarehouseInventoryItemDTO> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }
}



