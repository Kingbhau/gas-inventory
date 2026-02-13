export interface WarehouseInventoryItem {
  variantId: number;
  filledQty: number;
  emptyQty: number;
}

export interface WarehouseInventorySetupRequest {
  warehouseId: number;
  inventoryItems: WarehouseInventoryItem[];
}
