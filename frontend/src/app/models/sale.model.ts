import { SaleItem } from './sale-item.model';

export interface Sale {
  id?: number;
  referenceNumber: string;
  customerId: number;
  customerName: string;
  saleDate: string;
  totalAmount: number;
  amountReceived: number;
  paymentMode: string;
  bankAccountName?: string;
  saleItems: SaleItem[];
  createdBy?: string;
  createdDate?: string | Date;
  updatedBy?: string;
  updatedDate?: string | Date;
}
