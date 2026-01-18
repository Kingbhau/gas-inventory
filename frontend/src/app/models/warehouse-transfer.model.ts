export interface WarehouseTransfer {
  id?: number;
  referenceNumber?: string;
  fromWarehouseId: number;
  toWarehouseId: number;
  fromWarehouseName?: string;
  toWarehouseName?: string;
  variantId: number;
  variantName?: string;
  quantity: number;
  transferDate?: string;
  notes?: string;
  version?: number;
}

export interface WarehouseTransferDTO extends WarehouseTransfer {
}
