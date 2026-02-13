export interface BankAccountLedger {
  id: number;
  bankAccountId: number;
  bankAccountName: string;
  transactionType: string;
  amount: number;
  saleId: number;
  referenceNumber: string;
  description: string;
  transactionDate: string;
}

export interface BankAccountBalance {
  bankName: string;
  balance: number;
}

export interface BankAccountLedgerSummary {
  totalDeposits: number;
  totalWithdrawals: number;
  netBalance: number;
  balanceAfter: number;
  transactionCount: number;
  bankwiseBalances: BankAccountBalance[];
}
