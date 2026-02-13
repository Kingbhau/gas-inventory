export interface EmptyReturnRequest {
  warehouseId: number;
  customerId: number;
  variantId: number;
  emptyIn: number;
  amountReceived?: number;
  paymentMode?: string;
  bankAccountId?: number;
  transactionDate: string;
}
