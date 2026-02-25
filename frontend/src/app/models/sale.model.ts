import { SaleItem } from './sale-item.model';

export interface SalePaymentSplit {
  id?: number;
  paymentMode: string;
  amount: number;
  bankAccountId?: number;
  bankAccountName?: string;
  note?: string;
}

export interface Sale {
  id?: number;
  referenceNumber: string;
  customerId: number;
  customerName: string;
  warehouseId?: number;
  saleDate: string;
  totalAmount: number;
  amountReceived: number;
  paymentMode: string;
  bankAccountId?: number;
  bankAccountName?: string;
  paymentSplits?: SalePaymentSplit[];
  saleItems: SaleItem[];
  createdBy?: string;
  createdDate?: string | Date;
  updatedBy?: string;
  updatedDate?: string | Date;
}
