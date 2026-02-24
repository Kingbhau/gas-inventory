export interface CreateSupplierTransactionRequest {
  warehouseId: number;
  supplierId: number;
  variantId: number;
  transactionType?: string;
  filledReceived: number;
  emptyReceived?: number;
  filledSent?: number;
  emptySent: number;
  reference?: string;
  transactionDate?: string;
  amount?: number;
  note?: string;
}
