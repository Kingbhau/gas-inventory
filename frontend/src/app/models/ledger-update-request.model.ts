export interface LedgerUpdatePaymentSplit {
  modeOfPayment: string;
  amount: number;
  bankAccountId?: number;
  note?: string;
}

export interface LedgerUpdateRequest {
  filledOut?: number;
  emptyIn?: number;
  totalAmount?: number;
  amountReceived?: number;
  transactionDate?: string;
  updateReason?: string;
  paymentMode?: string;
  bankAccountId?: number;
  paymentSplits?: LedgerUpdatePaymentSplit[];
}
