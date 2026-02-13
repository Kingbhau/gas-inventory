export interface LedgerUpdateRequest {
  filledOut?: number;
  emptyIn?: number;
  totalAmount?: number;
  amountReceived?: number;
  updateReason?: string;
  paymentMode?: string;
  bankAccountId?: number;
}
