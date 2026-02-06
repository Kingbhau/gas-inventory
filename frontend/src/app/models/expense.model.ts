export interface Expense {
  id?: number;
  description: string;
  amount: number;
  category?: string;
  categoryId: number;
  expenseDate: string;
  notes?: string;
  paymentMode?: string;
  bankAccountId?: number;
  bankAccountName?: string;
  bankAccountNumber?: string;
  createdDate?: string;
  createdBy?: string;
  updatedDate?: string;
  updatedBy?: string;
}
