export interface PaymentModeStats {
  paymentModeName: string;
  paymentModeCode: string;
  totalAmount: number;
  transactionCount: number;
}

export interface PaymentModeSummary {
  paymentModeStats: Record<string, PaymentModeStats>;
  totalAmount: number;
  totalTransactions: number;
}
