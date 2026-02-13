export interface CustomerDuePayment {
  customerId: number;
  customerName: string;
  customerPhone: string;
  customerAddress: string;
  totalSalesAmount: number;
  amountReceived: number;
  dueAmount: number;
  lastTransactionDate: string;
  transactionCount: number;
}

export interface CustomerDuePaymentSummary {
  totalDueAmount: number;
  totalSalesAmount: number;
  totalAmountReceived: number;
  totalCustomersWithDue: number;
  averageDueAmount: number;
}
