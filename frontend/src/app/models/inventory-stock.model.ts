export interface InventoryStock {
  id?: number;
  variantId: number;
  variantName: string;
  warehouseId?: number;
  warehouseName?: string;
  filledQty: number;
  emptyQty: number;
  lastUpdated: string;
}
