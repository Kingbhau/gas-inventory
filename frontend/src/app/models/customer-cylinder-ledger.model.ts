export interface CustomerCylinderLedger {
  id?: number;
  customerId: number;
  customerName: string;
  variantId: number;
  variantName: string;
  transactionDate: string;
  refType: string;
  refId: number;
  filledOut: number;
  emptyIn: number;
  balance: number;
  transactionReference?: string;
  createdAt?: string;
  updatedDate?: string;
  createdBy?: string;
  updatedBy?: string;
  note?: string;
}
