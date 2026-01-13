export interface Expense {
  id?: number;
  description: string;
  amount: number;
  category?: string;
  categoryId: number;
  expenseDate: string;
  notes?: string;
  createdDate?: string;
  createdBy?: string;
}
