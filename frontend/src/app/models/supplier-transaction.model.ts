export interface SupplierTransaction {
  id?: number;
  supplierId: number;
  supplierName: string;
  warehouseId?: number;
  warehouseName?: string;
  variantId: number;
  variantName: string;
  transactionDate: string;
  filledReceived: number;
  emptySent: number;
  reference?: string;
  referenceNumber?: string;
  amount: number;
}
