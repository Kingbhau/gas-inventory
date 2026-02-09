export interface PaymentMode {
  id?: number;
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
  isBankAccountRequired?: boolean;
  createdDate?: string;
  createdBy?: string;
  updatedDate?: string;
  updatedBy?: string;
}
