export interface BankAccount {
  id?: number;
  bankName: string;
  accountNumber: string;
  accountHolderName: string;
  accountName?: string;
  accountType?: string;
  currentBalance: number;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}
