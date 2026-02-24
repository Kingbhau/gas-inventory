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
  emptyReceived?: number;
  filledSent?: number;
  emptySent: number;
  transactionType?: string;
  reference?: string;
  referenceNumber?: string;
  amount?: string | number; // Can be string or number from BigDecimal
  note?: string;
  createdBy?: string;
  createdDate?: string;
  updatedBy?: string;
  updatedDate?: string;
}
