export interface ExpenseFilters {
  fromDate?: string;
  toDate?: string;
  categoryId?: number;
  paymentMode?: string;
  bankAccountId?: number;
  minAmount?: number;
  maxAmount?: number;
  createdBy?: string;
}
