export interface CreateSupplierTransactionRequest {
  warehouseId: number;
  supplierId: number;
  variantId: number;
  filledReceived: number;
  emptySent: number;
  reference?: string;
  transactionDate?: string;
  amount: number;
}
