export interface DayBookPaymentSplit {
  id?: number;
  paymentMode: string;
  amount: number;
  bankAccountId?: number;
  bankAccountName?: string;
  note?: string;
}

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
  verificationStatus?: 'PENDING' | 'VERIFIED' | 'REJECTED';
  verifiedBy?: string;
  paymentSplits?: DayBookPaymentSplit[];
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
