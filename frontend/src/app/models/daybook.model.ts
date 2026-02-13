export interface DayBook {
  id: number;
  customerId: number;
  transactionDate: string;
  createdDate?: string;
  customerName?: string;
  warehouseName?: string;
  variantName?: string;
  filledCount?: number;
  emptyCount?: number;
  totalAmount?: number;
  amountReceived?: number;
  dueAmount?: number;
  paymentMode?: string;
  transactionType?: string;
  createdBy?: string;
  referenceNumber?: string;
  partyName?: string;
  details?: string;
}

export interface DayBookSummary {
  transactions: DayBook[];
  totalFilledCount: number;
  totalEmptyCount: number;
  totalAmount: number;
  totalAmountReceived: number;
  totalDueAmount: number;
  totalTransactions: number;
}
