export interface BankDeposit {
  id?: number;
  bankAccountId: number;
  bankName?: string;
  accountNumber?: string;
  depositDate: string; // YYYY-MM-DD
  depositAmount: number;
  referenceNumber: string;
  paymentMode: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}
