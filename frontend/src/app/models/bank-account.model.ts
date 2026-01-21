export interface BankAccount {
  id?: number;
  code?: string; // Auto-generated bank code (e.g., BANK001, BANK002)
  bankName: string;
  accountNumber: string;
  accountHolderName: string;
  accountName?: string;
  accountType?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}
