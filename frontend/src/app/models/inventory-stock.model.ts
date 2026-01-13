export interface InventoryStock {
  id?: number;
  variantId: number;
  variantName: string;
  filledQty: number;
  emptyQty: number;
  lastUpdated: string;
}
