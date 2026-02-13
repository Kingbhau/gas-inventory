export interface WarehouseTransfer {
  id?: number;
  referenceNumber?: string;
  fromWarehouseId: number;
  toWarehouseId: number;
  fromWarehouseName?: string;
  toWarehouseName?: string;
  variantId: number;
  variantName?: string;
  quantity?: number;
  filledQty?: number;
  emptyQty?: number;
  transferDate?: string;
  notes?: string;
  version?: number;
}

export interface WarehouseTransferDTO extends WarehouseTransfer {
}
