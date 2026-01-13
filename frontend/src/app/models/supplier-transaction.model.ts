export interface SupplierTransaction {
  id?: number;
  supplierId: number;
  supplierName: string;
  variantId: number;
  variantName: string;
  transactionDate: string;
  filledReceived: number;
  emptySent: number;
  reference: string;
  amount: number;
}
