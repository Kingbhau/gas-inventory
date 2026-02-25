export interface EmptyReturnPaymentSplitRequest {
  modeOfPayment: string;
  amount: number;
  bankAccountId?: number;
  note?: string;
}

export interface EmptyReturnRequest {
  warehouseId: number;
  customerId: number;
  variantId: number;
  emptyIn: number;
  amountReceived?: number;
  paymentType?: 'SINGLE' | 'MULTIPLE';
  paymentMode?: string;
  bankAccountId?: number;
  paymentSplits?: EmptyReturnPaymentSplitRequest[];
  transactionDate: string;
}
